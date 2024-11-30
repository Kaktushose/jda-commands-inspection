package com.github.kaktushose.jda.commands.inspection


import com.github.kaktushose.jda.commands.annotations.interactions.DynamicOptions
import com.github.kaktushose.jda.commands.dispatching.reply.ModalReplyable
import com.github.kaktushose.jda.commands.dispatching.reply.Replyable
import com.github.kaktushose.jda.commands.dispatching.reply.components.Buttons
import com.github.kaktushose.jda.commands.dispatching.reply.components.SelectMenus
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.Nls

class MethodReferenceInspection : AbstractBaseJavaLocalInspectionTool() {

    override fun getDisplayName(): String {
        return "JDA-Commands method referencing"
    }

    override fun checkFile(
        file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        if (file !is PsiJavaFile) return ProblemDescriptor.EMPTY_ARRAY

        val problems = mutableListOf<ProblemDescriptor>()

        file.accept(object : JavaRecursiveElementVisitor() {
            override fun visitLiteralExpression(expression: PsiLiteralExpression) {
                super.visitLiteralExpression(expression)

                val value = expression.value
                if (value !is String) {
                    return
                }

                val parentAnnotation = PsiTreeUtil.getParentOfType(expression, PsiAnnotation::class.java)
                val parentMethodCall = PsiTreeUtil.getParentOfType(expression, PsiMethodCallExpression::class.java)

                if (parentAnnotation != null) {
                    if (parentAnnotation.qualifiedName == DynamicOptions::class.qualifiedName) {
                        checkMethodExists(
                            value,
                            expression,
                            manager,
                            isOnTheFly,
                            problems,
                            "DynamicOptionResolver",
                            CreateDynamicOptionResolverQuickFix(value)
                        )
                    }
                }

                if (parentMethodCall == null) {
                    return
                }
                val selectMethods = listOf(
                    SelectMenus::class.simpleName + "." + SelectMenus::enabled.name,
                    SelectMenus::class.simpleName + "." + SelectMenus::disabled.name,
                )
                if (selectMethods.contains(parentMethodCall.methodExpression.qualifiedName) ||
                    parentMethodCall.methodExpression.referenceName == Replyable::withSelectMenus.name
                ) {
                    checkMethodExists(
                        value,
                        expression,
                        manager,
                        isOnTheFly,
                        problems,
                        "SelectMenu",
                        CreateStringMenuQuickFix(value),
                        CreateEntityMenuQuickFix(value)
                    )
                }
                val buttonMethods = listOf(
                    Buttons::class.simpleName + "." + Buttons::enabled.name,
                    Buttons::class.simpleName + "." + Buttons::disabled.name,
                )
                if (buttonMethods.contains(parentMethodCall.methodExpression.qualifiedName) ||
                    parentMethodCall.methodExpression.referenceName == Replyable::withButtons.name
                ) {
                    checkMethodExists(
                        value, expression, manager, isOnTheFly, problems, "Button", CreateButtonQuickFix(value)
                    )
                }
                if (parentMethodCall.methodExpression.referenceName == ModalReplyable::replyModal.name) {
                    checkMethodExists(
                        value, expression, manager, isOnTheFly, problems, "Modal", CreateModalQuickFix(value)
                    )
                }
            }


        })
        return problems.toTypedArray()
    }

    fun getParentClassName(methodCallExpression: PsiMethodCallExpression): String? {
        val containingClass: PsiClass? = PsiTreeUtil.getParentOfType(methodCallExpression, PsiClass::class.java)
        return containingClass?.name
    }

    private fun checkMethodExists(
        methodName: String,
        expression: PsiLiteralExpression,
        manager: InspectionManager,
        isOnTheFly: Boolean,
        problems: MutableList<ProblemDescriptor>,
        type: String,
        vararg quickFix: AbstractQuickFix
    ) {
        val containingClass = PsiTreeUtil.getParentOfType(expression, PsiClass::class.java) ?: return
        if (containingClass.methods.any { it.name == methodName }) {
            return
        }
        problems.add(
            manager.createProblemDescriptor(
                expression,
                "'$type' '$methodName' does not exist in class '${containingClass.name}'",
                quickFix,
                ProblemHighlightType.ERROR,
                isOnTheFly,
                false
            )
        )
    }


    private abstract class AbstractQuickFix(private val methodName: String, private val type: String) : LocalQuickFix {
        @Nls(capitalization = Nls.Capitalization.Sentence)
        override fun getName(): String = "Create '$type' '$methodName'"

        override fun getFamilyName(): String = "Method reference inspector"
    }

    private class CreateDynamicOptionResolverQuickFix(private val methodName: String) :
        AbstractQuickFix(methodName, "DynamicOptionResolver") {
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val containingClass = PsiTreeUtil.getParentOfType(descriptor.psiElement, PsiClass::class.java)
            val method = JavaPsiFacade.getElementFactory(project).createMethodFromText(
                """
                @DynamicOptionResolver
                public Collection<SelectOption> $methodName() {
                    return Collections.emptySet();
                }
                """.trimIndent(), containingClass
            )

            containingClass?.add(method)
        }
    }

    private class CreateModalQuickFix(private val methodName: String) : AbstractQuickFix(methodName, "Modal") {
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val containingClass = PsiTreeUtil.getParentOfType(descriptor.psiElement, PsiClass::class.java)
            val method = JavaPsiFacade.getElementFactory(project).createMethodFromText(
                """
                @Modal()
                public void $methodName(ModalEvent event, @TextInput String string) {
                    
                }
                """.trimIndent(), containingClass
            )

            containingClass?.add(method)
        }
    }

    private class CreateStringMenuQuickFix(private val methodName: String) :
        AbstractQuickFix(methodName, "StringSelectMenu") {
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val containingClass = PsiTreeUtil.getParentOfType(descriptor.psiElement, PsiClass::class.java)
            val method = JavaPsiFacade.getElementFactory(project).createMethodFromText(
                """
                @StringSelectMenu()
                public void $methodName(ComponentEvent event) {
                    
                }
                """.trimIndent(), containingClass
            )

            containingClass?.add(method)
        }
    }

    private class CreateEntityMenuQuickFix(private val methodName: String) :
        AbstractQuickFix(methodName, "EntitySelectMenu") {
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val containingClass = PsiTreeUtil.getParentOfType(descriptor.psiElement, PsiClass::class.java)
            val method = JavaPsiFacade.getElementFactory(project).createMethodFromText(
                """
                @EntitySelectMenu()
                public void $methodName(ComponentEvent event) {
                    
                }
                """.trimIndent(), containingClass
            )

            containingClass?.add(method)
        }
    }

    private class CreateButtonQuickFix(private val methodName: String) : AbstractQuickFix(methodName, "Button") {
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val containingClass = PsiTreeUtil.getParentOfType(descriptor.psiElement, PsiClass::class.java)
            val method = JavaPsiFacade.getElementFactory(project).createMethodFromText(
                """
                @Button()
                public void $methodName(ComponentEvent event) {
                    
                }
                """.trimIndent(), containingClass
            )

            containingClass?.add(method)
        }
    }
}
