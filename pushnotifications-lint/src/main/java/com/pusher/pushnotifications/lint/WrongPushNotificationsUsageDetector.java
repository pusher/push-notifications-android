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


    @Override
    public List<String> getApplicableConstructorTypes() {
        return Collections.singletonList("com.pusher.pushnotifications.PushNotificationsInstance");
    }

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList("start");
    }

    @Override
    public void visitConstructor(JavaContext context, UCallExpression call, PsiMethod method) {
        reportBadInstanceIdIssue(context, call);
    }

    @Override
    public void visitMethod(JavaContext context, UCallExpression call, PsiMethod method) {
        JavaEvaluator evaluator = context.getEvaluator();

        if (evaluator.isMemberInClass(method, "com.pusher.pushnotifications.PushNotifications")) {
            reportBadInstanceIdIssue(context, call);
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
                ISSUE_BAD_INSTANCE_ID
        };
    }
}
