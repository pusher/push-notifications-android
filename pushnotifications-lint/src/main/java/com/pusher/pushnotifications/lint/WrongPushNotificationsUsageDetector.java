package com.pusher.pushnotifications.lint;

import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiMethod;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class WrongPushNotificationsUsageDetector extends Detector implements Detector.UastScanner {
    private static final String INCORRECT_MESSAGE_FORMAT = "The instance id argument looks incorrect. It should be something like '%s'.";
    private static final int ISSUE_BAD_INSTANCE_ID_PRIORITY = 10;
    private static final Issue ISSUE_BAD_INSTANCE_ID =
            Issue.create(
                    "BadPushNotificationsInstanceId",
                    "Checks the Instance Id argument for Push Notifications",
                    "The Instance Id for Push Notifications follows a specific format, so unrecognizable values are likely to be wrong.",
                    Category.MESSAGES,
                    ISSUE_BAD_INSTANCE_ID_PRIORITY,
                    Severity.WARNING,
                    new Implementation(WrongPushNotificationsUsageDetector.class, Scope.JAVA_FILE_SCOPE)
            );

    private static final int MAX_INTEREST_NAME_LENGTH = 164;
    private static final String INTEREST_NAME_TOO_LONG_FORMAT = "This interest name is too long (%d characters). It can only have at most " + MAX_INTEREST_NAME_LENGTH + " characters.";
    private static final int ISSUE_INTEREST_NAME_TOO_LONG_PRIORITY = 10;
    private static final Issue ISSUE_INTEREST_NAME_TOO_LONG =
            Issue.create(
                    "PushNotificationsInterestNameTooLong",
                    "Checks the length of the interest name for Push Notifications",
                    "The length of an interest name has a defined maximum length.",
                    Category.MESSAGES,
                    ISSUE_INTEREST_NAME_TOO_LONG_PRIORITY,
                    Severity.WARNING,
                    new Implementation(WrongPushNotificationsUsageDetector.class, Scope.JAVA_FILE_SCOPE)
            );

    private static final String VALID_INTEREST_NAME_REGEX = "^[a-zA-Z0-9_\\-=@,.;]+$";
    private static final String INCORRECT_INTEREST_NAME = "This interest name contains invalid characters. It can only be ASCII upper/lower-case letters, numbers and one of _-=@,.:";
    private static final int ISSUE_INCORRECT_INTEREST_NAME_PRIORITY = 10;
    private static final Issue ISSUE_INCORRECT_INTEREST_NAME =
            Issue.create(
                    "BadPushNotificationsInterestName",
                    "Checks if the interest is valid",
                    "Interest names have to respect the following regex: `" + VALID_INTEREST_NAME_REGEX + "`.",
                    Category.MESSAGES,
                    ISSUE_INCORRECT_INTEREST_NAME_PRIORITY,
                    Severity.WARNING,
                    new Implementation(WrongPushNotificationsUsageDetector.class, Scope.JAVA_FILE_SCOPE)
            );


    @Override
    public List<String> getApplicableConstructorTypes() {
        return Collections.singletonList("com.pusher.pushnotifications.PushNotificationsInstance");
    }

    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList("start", "subscribe", "unsubscribe");
    }

    @Override
    public void visitConstructor(JavaContext context, UCallExpression call, PsiMethod method) {
        reportBadInstanceIdIssue(context, call);
    }

    @Override
    public void visitMethod(JavaContext context, UCallExpression call, PsiMethod method) {
        JavaEvaluator evaluator = context.getEvaluator();

        if (evaluator.isMemberInClass(method, "com.pusher.pushnotifications.PushNotifications")) {
            if (method.getName().equals("start")) {
                reportBadInstanceIdIssue(context, call);
            } else {
                reportInterestNameTooLongIssue(context, call);
                reportBadInterestNameIssue(context, call);
            }
        }
    }

    private static void reportBadInterestNameIssue(JavaContext context, UCallExpression call) {
        List<UExpression> arguments = call.getValueArguments();

        if (arguments.size() == 1) {
            Object possibleInterestName = arguments.get(0).evaluate();
            if (possibleInterestName != null) {
                String interestName = possibleInterestName.toString();
                if (!interestName.matches(VALID_INTEREST_NAME_REGEX)) {
                    LintFix.GroupBuilder fixGrouper = fix().group();
                    LintFix fix = fixGrouper.build();

                    context.report(ISSUE_INCORRECT_INTEREST_NAME, call, context.getLocation(arguments.get(0)), INCORRECT_INTEREST_NAME, fix);
                }
            }
        }
    }

    private static void reportInterestNameTooLongIssue(JavaContext context, UCallExpression call) {
        List<UExpression> arguments = call.getValueArguments();

        if (arguments.size() == 1) {
            Object possibleInterestName = arguments.get(0).evaluate();
            if (possibleInterestName != null) {
                String interestName = possibleInterestName.toString();
                if (interestName.length() > MAX_INTEREST_NAME_LENGTH) {
                    LintFix.GroupBuilder fixGrouper = fix().group();
                    LintFix fix = fixGrouper.build();

                    String message = String.format(INTEREST_NAME_TOO_LONG_FORMAT, interestName.length());
                    context.report(ISSUE_INTEREST_NAME_TOO_LONG, call, context.getLocation(arguments.get(0)), message, fix);
                }
            }
        }
    }

    private static void reportBadInstanceIdIssue(JavaContext context, UCallExpression call) {
        List<UExpression> arguments = call.getValueArguments();

        if (arguments.size() == 2) {
            Object possibleInstanceId = arguments.get(1).evaluate();
            if (possibleInstanceId != null && possibleInstanceId != "") {
                try {
                    UUID.fromString(possibleInstanceId.toString());
                } catch (Exception e) {
                    LintFix.GroupBuilder fixGrouper = fix().group();
                    LintFix fix = fixGrouper.build();

                    String message = String.format(INCORRECT_MESSAGE_FORMAT, UUID.randomUUID());
                    context.report(ISSUE_BAD_INSTANCE_ID, call, context.getLocation(arguments.get(1)), message, fix);
                }
            }
        }
    }

    static Issue[] getIssues() {
        return new Issue[]{
                ISSUE_BAD_INSTANCE_ID,
                ISSUE_INTEREST_NAME_TOO_LONG,
                ISSUE_INCORRECT_INTEREST_NAME
        };
    }
}
