/*
 * Copyright (c) 2017 ATS Advanced Telematic Systems GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.advancedtelematic.ota.deviceregistry.db

import com.advancedtelematic.libats.messaging_datatype.DataType.DeviceId
import com.advancedtelematic.libats.data.DataType.Namespace
import com.advancedtelematic.libats.data.PaginationResult
import com.advancedtelematic.libats.slick.db.SlickAnyVal._
import com.advancedtelematic.libats.slick.db.SlickExtensions._
import com.advancedtelematic.libats.slick.db.SlickUUIDKey._
import com.advancedtelematic.ota.deviceregistry.common.Errors
import com.advancedtelematic.ota.deviceregistry.data.Group.GroupId
import com.advancedtelematic.ota.deviceregistry.data.{Device, GroupExpressionAST, GroupType}
import com.advancedtelematic.ota.deviceregistry.db.DbOps.PaginationResultOps
import slick.jdbc.MySQLProfile.api._
import slick.lifted.Tag

import scala.concurrent.ExecutionContext

object GroupMemberRepository {

  final case class GroupMember(groupId: GroupId, deviceUuid: DeviceId)

  // scalastyle:off
  class GroupMembersTable(tag: Tag)
      extends Table[GroupMember](tag, "GroupMembers") {
    def groupId    = column[GroupId]("group_id")
    def deviceUuid = column[DeviceId]("device_uuid")

    def pk = primaryKey("pk_group_members", (groupId, deviceUuid))

    def * =
      (groupId, deviceUuid) <>
      ((GroupMember.apply _).tupled, GroupMember.unapply)
  }
  // scalastyle:on

  val groupMembers = TableQuery[GroupMembersTable]

  //this method assumes that groupId and deviceId belong to the same namespace
  def addGroupMember(groupId: GroupId, deviceId: DeviceId)(implicit ec: ExecutionContext): DBIO[Int] =
    (groupMembers += GroupMember(groupId, deviceId))
      .handleIntegrityErrors(Errors.MemberAlreadyExists)

  def removeGroupMember(groupId: GroupId, deviceId: DeviceId)
                       (implicit ec: ExecutionContext): DBIO[Unit] =
    groupMembers
      .filter(r => r.groupId === groupId && r.deviceUuid === deviceId)
      .delete
      .handleSingleUpdateError(Errors.MissingGroup)

  def removeGroupMemberAll(deviceUuid: DeviceId)(implicit ec: ExecutionContext): DBIO[Int] =
    groupMembers
      .filter(_.deviceUuid === deviceUuid)
      .delete

  def listDevicesInGroup(groupId: GroupId, offset: Option[Long], limit: Option[Long])
                        (implicit db: Database, ec: ExecutionContext): DBIO[PaginationResult[DeviceId]] =
    listDevicesInGroupAction(groupId, offset, limit)

  def listDevicesInGroupAction(groupId: GroupId, offset: Option[Long], limit: Option[Long])
                              (implicit ec: ExecutionContext): DBIO[PaginationResult[DeviceId]] =
    groupMembers
      .filter(_.groupId === groupId)
      .map(_.deviceUuid)
      .paginateResult(offset.orDefaultOffset, limit.orDefaultLimit)

  def countDevicesInGroup(
      groupId: GroupId
  )(implicit ec: ExecutionContext): DBIO[Long] =
    listDevicesInGroupAction(groupId, None, None).map(_.total)

  def deleteDynamicGroupsForDevice(namespace: Namespace, deviceUuid: DeviceId)(
      implicit ec: ExecutionContext
  ): DBIO[Unit] =
    groupMembers
      .filter { _.groupId.in(GroupInfoRepository.groupInfos.filter(_.groupType === GroupType.dynamic).map(_.id)) }
      .delete
      .map(_ => ())

  def addDeviceToDynamicGroups(namespace: Namespace, device: Device)(implicit ec: ExecutionContext): DBIO[Unit] = {
    val dynamicGroupIds =
      GroupInfoRepository.groupInfos
        .filter { g => (g.groupType === GroupType.dynamic) && (g.namespace === namespace) }
        .result.map {
        _.filter { group =>
          val compiledExp = GroupExpressionAST.compileToScala(group.expression.get)
          compiledExp.apply(device)
        }
      }

    dynamicGroupIds.flatMap { groups =>
      DBIO.sequence(groups.map(group => GroupMemberRepository.addGroupMember(group.id, device.uuid)))
    }.map(_ => ())
  }

  def listGroupsForDevice(namespace: Namespace, deviceUuid: DeviceId, offset: Option[Long], limit: Option[Long])
                         (implicit ec: ExecutionContext): DBIO[PaginationResult[GroupId]] =
    groupMembers
      .filter(_.deviceUuid === deviceUuid)
      .map(_.groupId)
      .paginateResult(offset.orDefaultOffset, limit.orDefaultLimit)
}
