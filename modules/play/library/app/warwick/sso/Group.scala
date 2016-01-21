package warwick.sso

import org.joda.time.DateTime

import scala.collection.JavaConversions._

case class GroupName(string: String)

case class Group(
  name: GroupName,
  title: Option[String],
  members: Seq[Usercode],
  owners: Seq[Usercode],
  `type`: String,
  department: Department,
  updatedAt: DateTime
) {

  def contains(usercode: Usercode): Boolean = owners.contains(usercode) || members.contains(usercode)

}

object Group {

  def apply(g: uk.ac.warwick.userlookup.Group): Group =
    Group(
      GroupName(g.getName),
      Option(g.getTitle),
      g.getUserCodes.map(Usercode),
      g.getOwners.map(Usercode),
      g.getType,
      Department(None, Some(g.getDepartment), Some(g.getDepartmentCode)),
      new DateTime(g.getLastUpdatedDate)
    )

}
