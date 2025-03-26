# Permissions

This documentation outlines the various permissions required across different areas of the Dulno Panel. Permissions follow the schema **`<Scope>.<Action>`**.

---

## Table of Contents
1. [Asterisk](#asterisk)
2. [Tickets](#tickets)
3. [Questions](#questions)
4. [Sales](#sales)
5. [Partners](#partners)
6. [Members](#members)
7. [Offers](#offers)
8. [Maintenance](#maintenance)
9. [Cluster](#cluster)
10. [Groups](#groups)
11. [Staff](#staff)

---

## 1. Asterisk

The asterisk permission (*) can be understood as a wildcard. Whether member or group: every entity with the asterisk authorization has no restrictions and has all authorizations.

---

## 2. Tickets

The Tickets section contains permissions required for listing, viewing, answering, and closing tickets.

| Permission              | Description                          |
|-------------------------|--------------------------------------|
| `tickets.find`          | Allows listing all pending tickets.  |
| `ticket.find`           | Allows viewing an individual ticket. |
| `ticket.answer`         | Allows answering a ticket.           |
| `ticket.message.delete` | Allows deleting a ticket message.    |
| `ticket.close`          | Allows closing a ticket.             |

---

## 3. Questions

The Questions section contains permissions required for listing, viewing, answering, and closing questions.

| Permission        | Description                            |
|-------------------|----------------------------------------|
| `questions.find`  | Allows listing all pending questions.  |
| `question.find`   | Allows viewing an individual question. |
| `question.answer` | Allows answering a question.           |
| `question.close`  | Allows closing a question.             |

---

## 4. Sales

The Sales section contains permissions required for listing, viewing, answering, and closing sales.

| Permission    | Description                        |
|---------------|------------------------------------|
| `sales.find`  | Allows listing all pending sales.  |
| `sale.find`   | Allows viewing an individual sale. |
| `sale.answer` | Allows answering a sale.           |
| `sale.close`  | Allows closing a sale.             |

---

## 5. Partners

The Partners section contains permissions required for viewing and editing the bundle and payments of the partners.

| Permission                   | Description                                            |
|------------------------------|--------------------------------------------------------|
| `partners.find`              | Allows listing all partners.                           |
| `partner.find`               | Allows viewing an individual partner.                  |
| `partner.bundle.find`        | Allows viewing the bundle of a partner.                |
| `partner.bundle.change`      | Allows changing the bundle of a partner.               |
| `partner.bundle.delete`      | Allows deleting the bundle of a partner.               |
| `partner.payments.find`      | Allows viewing the payments of a partner.              |
| `partner.termination.status` | Allows retrieving the termination status of a partner. |
| `partner.bundle.terminate`   | Allows terminate the bundle of a partner.              |

---

## 6. Members

The Members section contains permissions required for viewing members, viewing and resending member mails and viewing and closing member sessions.

| Permission             | Description                                       |
|------------------------|---------------------------------------------------|
| `member.find`          | Allows viewing an individual member.              |
| `member.mails.find`    | Allows viewing all mails send to the member.      |
| `member.mail.resend`   | Allows resending a mail to the member.            |
| `member.sessions.find` | Allows viewing all active sessions of the member. |
| `member.session.close` | Allows closing an member session.                 |

---

## 7. Offers

The Offers section contains permissions required for listing, viewing, creating and withdrawing offers.

| Permission               | Description                                        |
|--------------------------|----------------------------------------------------|
| `offers.find`            | Allows listing all offers of an entity.            |
| `offer.find`             | Allows viewing an individual offer.                |
| `offer.price.suggestion` | Allows generating a price suggestion for an offer. |
| `offer.create`           | Allows creating a new offer.                       |
| `offer.withdraw`         | Allows withdrawing an offer.                       |

---

## 8. Maintenance

The Maintenance section contains permissions required for listing, creating, finishing and deleting maintenance.

| Permission           | Description                                        |
|----------------------|----------------------------------------------------|
| `maintenance.find`   | Allows listing all pending and active maintenance. |
| `maintenance.create` | Allows creating a maintenance.                     |
| `maintenance.finish` | Allows finishing a maintenance.                    |
| `maintenance.delete` | Allows deleting a maintenance.                     |

## 9. Cluster

The Cluster section contains permissions required for monitoring and editing the state of the cluster.

| Permission                 | Description                                       |
|----------------------------|---------------------------------------------------|
| `cluster.resources`        | Allows viewing the used resources of the cluster. |
| `cluster.servers`          | Allows listing all servers of the cluster.        |
| `cluster.server`           | Allows viewing an individual cluster server.      |
| `cluster.units`            | Allows listing all units of the cluster.          |
| `cluster.unit`             | Allows viewing an individual cluster unit.        |
| `cluster.unit.restart`     | Allows restarting an individual cluster unit.     |
| `cluster.unit.scale`       | Allows scaling an individual cluster unit.        |
| `cluster.pod`              | Allows viewing an individual cluster pod.         |
| `cluster.pod.delete`       | Allows deleting an individual cluster pod.        |
| `cluster.backups`          | Allows listing all backups of the cluster.        |
| `cluster.backup.download`  | Allows downloading an individual cluster backup.  |
| `cluster.backup.upload`    | Allows uploading an individual cluster backup.    |
| `cluster.backup.restore`   | Allows restoring an individual cluster backup.    |
| `cluster.backup.delete`    | Allows deleting an individual cluster backup.     |
| `cluster.errors`           | Allows listing all errors of the cluster.         |
| `cluster.error.fixed`      | Allows fixing an individual cluster error.        |
| `cluster.errors.fixed.all` | Allows fixing all errors of the cluster.          |

---

## 10. Groups

The Groups section contains permissions required for listing, adding, removing, renaming and editing groups.

| Permission                | Description                                |
|---------------------------|--------------------------------------------|
| `groups.find`             | Allows listing all groups.                 |
| `group.add`               | Allows adding a new group.                 |
| `group.remove`            | Allows removing a group.                   |
| `group.rename`            | Allows renaming a group.                   |
| `group.priority.change`   | Allows changing the priority of a group.   |
| `group.color.change`      | Allows changing the color of a group.      |
| `group.permission.add`    | Allows adding a permission to a group.     |
| `group.permission.remove` | Allows removing a permission from a group. |

---

## 11. Staff

The Staff section contains permissions required for listing, adding, removing and editing staff.

| Permission                | Description                                |
|---------------------------|--------------------------------------------|
| `staff.find`              | Allows listing all staff.                  |
| `staff.add`               | Allows adding a new staff.                 |
| `staff.remove`            | Allows removing a staff.                   |
| `staff.group.change`      | Allows changing the group of a staff.      |
| `staff.permission.add`    | Allows adding a permission to a staff.     |
| `staff.permission.remove` | Allows removing a permission from a staff. |

---

## Notes

- **Fine-grained Access Control**: Depending on specific needs, a more detailed permission configuration may be required for certain roles.
- **Least Privilege Principle**: Permissions should be granted according to the **principle of least privilege**, ensuring staff have only the access they need to perform their tasks.

---