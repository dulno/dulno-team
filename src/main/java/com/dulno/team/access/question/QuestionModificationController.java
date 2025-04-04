package com.dulno.team.access.question;

import com.dulno.core.request.DulnoRequestBody;
import com.dulno.core.mail.Mail;
import com.dulno.core.question.Question;
import com.dulno.core.question.QuestionDatabaseTable;
import com.dulno.core.question.QuestionMessageDatabaseTable;
import com.dulno.core.question.QuestionMessageSenderType;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.staff.Staff;
import com.dulno.team.staff.StaffDatabaseTable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.UUID;

@RestController
public final class QuestionModificationController extends TeamController {
  private final Mail supportMail;
  private final QuestionDatabaseTable questionDatabaseTable;
  private final QuestionMessageDatabaseTable questionMessageDatabaseTable;

  private QuestionModificationController(
    Key secretKey, StaffDatabaseTable staffDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    @Qualifier("supportMail") Mail supportMail,
    QuestionDatabaseTable questionDatabaseTable,
    QuestionMessageDatabaseTable questionMessageDatabaseTable
  ) {
    super(secretKey, staffDatabaseTable, groupDatabaseTable);
    this.supportMail = supportMail;
    this.questionDatabaseTable = questionDatabaseTable;
    this.questionMessageDatabaseTable = questionMessageDatabaseTable;
  }

  @RequestMapping(path = "/question/close/", method = RequestMethod.POST)
  public void closeQuestion(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var questionId = body.getUUID("question");
    staffHasPermission(findStaffId(request), "question.close")
      .thenAccept(hasPermission -> questionDatabaseTable.questionExists(questionId)
        .thenAccept(exists -> closeQuestion(questionId, hasPermission, exists)));
  }

  private void closeQuestion(
    UUID questionId, boolean hasPermission, boolean exists
  ) {
    if (!hasPermission || !exists) {
      return;
    }
    questionDatabaseTable.updateQuestionStatus(questionId, Question.Status.CLOSED);
  }

  @RequestMapping(path = "/question/answer/", method = RequestMethod.POST)
  public void answerQuestion(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var questionId = body.getUUID("question");
    findStaff(request).thenAccept(staff ->
      staffHasPermission(staff.id(), "question.answer")
        .thenAccept(hasPermission -> questionDatabaseTable.questionExists(questionId)
          .thenAccept(exists -> answerQuestion(staff, questionId,
            body.getString("message"), hasPermission, exists))));
  }

  private void answerQuestion(
    Staff staff, UUID questionId, String message, boolean hasPermission,
    boolean exists
  ) {
    if (!hasPermission || !exists) {
      return;
    }
    questionDatabaseTable.findQuestion(questionId).thenAccept(question ->
      questionMessageDatabaseTable.generateAvailableMessageId().thenAccept(id ->
        answerQuestion(staff, question, message, id)));
  }

  private void answerQuestion(
    Staff staff, Question question, String message, UUID messageId
  ) {
    question.resetExpiration();
    questionDatabaseTable.updateQuestion(question);
    supportMail.send(question.sender(), "Re: " + question.title(), message,
      "text/html; charset=utf-8")
      .thenAccept(publicId -> questionMessageDatabaseTable.insertQuestionMessage(
        messageId, publicId, question.id(), staff.id().toString(),
        QuestionMessageSenderType.TEAM, message, System.currentTimeMillis()));
  }
}
