/********************************************************************************
 * Copyright (c) 2018 Contributors to the Gamma project
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package hu.bme.mit.gamma.action.language.validation;

import java.util.Collection;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.validation.Check;

import hu.bme.mit.gamma.action.model.Action;
import hu.bme.mit.gamma.action.model.ActionModelPackage;
import hu.bme.mit.gamma.action.model.AssignmentStatement;
import hu.bme.mit.gamma.action.model.Block;
import hu.bme.mit.gamma.action.model.Branch;
import hu.bme.mit.gamma.action.model.BreakStatement;
import hu.bme.mit.gamma.action.model.ChoiceStatement;
import hu.bme.mit.gamma.action.model.ConstantDeclarationStatement;
import hu.bme.mit.gamma.action.model.ExpressionStatement;
import hu.bme.mit.gamma.action.model.ForStatement;
import hu.bme.mit.gamma.action.model.IfStatement;
import hu.bme.mit.gamma.action.model.ProcedureDeclaration;
import hu.bme.mit.gamma.action.model.ReturnStatement;
import hu.bme.mit.gamma.action.model.SwitchStatement;
import hu.bme.mit.gamma.action.model.VariableDeclarationStatement;
import hu.bme.mit.gamma.expression.language.validation.ExpressionType;
import hu.bme.mit.gamma.expression.model.AccessExpression;
import hu.bme.mit.gamma.expression.model.ArrayAccessExpression;
import hu.bme.mit.gamma.expression.model.EnumerationLiteralExpression;
import hu.bme.mit.gamma.expression.model.EnumerationTypeDefinition;
import hu.bme.mit.gamma.expression.model.ExpressionModelPackage;
import hu.bme.mit.gamma.expression.model.FunctionAccessExpression;
import hu.bme.mit.gamma.expression.model.LambdaDeclaration;
import hu.bme.mit.gamma.expression.model.NamedElement;
import hu.bme.mit.gamma.expression.model.RecordAccessExpression;
import hu.bme.mit.gamma.expression.model.ReferenceExpression;
import hu.bme.mit.gamma.expression.model.SelectExpression;
import hu.bme.mit.gamma.expression.model.Type;
import hu.bme.mit.gamma.expression.model.TypeReference;
import hu.bme.mit.gamma.expression.model.ValueDeclaration;
import hu.bme.mit.gamma.expression.model.VariableDeclaration;

/**
 * This class contains custom validation rules. 
 *
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation
 */
public class ActionLanguageValidator extends AbstractActionLanguageValidator {
	
	@Check
	public void checkNameUniqueness(NamedElement element) {
		Collection<? extends NamedElement> namedElements = ActionLanguageValidatorUtil.getRecursiveContainerNamedElements(element);
		while(namedElements.remove(element)) {}
		for (NamedElement elem : namedElements) {
			if (element.getName().equals(elem.getName())) {
				error("Names must be unique!!!", ExpressionModelPackage.Literals.NAMED_ELEMENT__NAME);
			}
		}
	}
	
	//TODO remove
	@Check
	public void checkUnsupportedActions(Action action) {
		if (action instanceof Block ||
				action instanceof BreakStatement ||
				action instanceof ChoiceStatement ||
				action instanceof ConstantDeclarationStatement ||
				action instanceof ExpressionStatement ||
				action instanceof ForStatement ||
				action instanceof IfStatement ||
				action instanceof ReturnStatement ||
				action instanceof SwitchStatement ||
				action instanceof VariableDeclarationStatement) {
			EObject container = action.eContainer();
			EReference eContainmentFeature = action.eContainmentFeature();
			Object object = container.eGet(eContainmentFeature, true);
			if (object instanceof List) {
				@SuppressWarnings("unchecked")
				List<Action> actions = (List<Action>) object;
				int index = actions.indexOf(action);
				//error("Not supported action.", container, eContainmentFeature, index);
			}
			else {
				//error("Not supported action.", container, eContainmentFeature);
			}
		}
	}
	
	@Check
	public void checkAssignmentActions(AssignmentStatement assignment) {
		ReferenceExpression reference = (ReferenceExpression) assignment.getLhs();
		//Constant
		if (!(reference.getDeclaration() instanceof VariableDeclaration)) {
			error("Values can be assigned only to variables.", ActionModelPackage.Literals.ASSIGNMENT_STATEMENT__LHS);
		}
		//Variable
		ExpressionType rightHandSideExpressionType = typeDeterminator.getType(assignment.getRhs());
		if (reference instanceof ArrayAccessExpression) {
			ExpressionType leftHandSideExpressionType = typeDeterminator.getType((ArrayAccessExpression)reference);
			if (!(leftHandSideExpressionType == rightHandSideExpressionType)) {
				error("The types of the variable declaration and the right hand side expression are not the same: " +
						leftHandSideExpressionType.toString().toLowerCase() + " and " +
						rightHandSideExpressionType.toString().toLowerCase() + ".",
						ActionModelPackage.Literals.ASSIGNMENT_STATEMENT__LHS);
			}
		} else if (reference instanceof RecordAccessExpression) {
			ExpressionType leftHandSideExpressionType = typeDeterminator.getType((RecordAccessExpression)reference);
			if (!(leftHandSideExpressionType == rightHandSideExpressionType)) {
				error("The types of the variable declaration and the right hand side expression are not the same: " +
						leftHandSideExpressionType.toString().toLowerCase() + " and " +
						rightHandSideExpressionType.toString().toLowerCase() + ".",
						ActionModelPackage.Literals.ASSIGNMENT_STATEMENT__LHS);
			}
		} else if (reference instanceof SelectExpression){
			error("The left hand side of the assignment must be an assignable variable. It is currently a non-deterministic selection from an enumerable type.",
					ActionModelPackage.Literals.ASSIGNMENT_STATEMENT__LHS);
		} else if (reference instanceof FunctionAccessExpression) {
			error("The left hand side of the assignment must be an assignable variable. It is currently a function call.",
					ActionModelPackage.Literals.ASSIGNMENT_STATEMENT__LHS);
		} else {
			VariableDeclaration variableDeclaration = (VariableDeclaration) reference.getDeclaration();
			try {
				Type variableDeclarationType = variableDeclaration.getType();
				if (!typeDeterminator.equals(variableDeclarationType, rightHandSideExpressionType)) {
					error("The types of the variable declaration and the right hand side expression are not the same: " +
							typeDeterminator.transform(variableDeclarationType).toString().toLowerCase() + " and " +
							rightHandSideExpressionType.toString().toLowerCase() + ".",
							ActionModelPackage.Literals.ASSIGNMENT_STATEMENT__LHS);
				}
				// Additional checks for enumerations
				EnumerationTypeDefinition enumType = null;
				if (variableDeclarationType instanceof EnumerationTypeDefinition) {
					enumType = (EnumerationTypeDefinition) variableDeclarationType;
				}
				else if (variableDeclarationType instanceof TypeReference &&
						((TypeReference) variableDeclarationType).getReference().getType() instanceof EnumerationTypeDefinition) {
					enumType = (EnumerationTypeDefinition) ((TypeReference) variableDeclarationType).getReference().getType();
				}
				if (enumType != null) {
					if (assignment.getRhs() instanceof EnumerationLiteralExpression) {
						EnumerationLiteralExpression rhs = (EnumerationLiteralExpression) assignment.getRhs();
						if (!enumType.getLiterals().contains(rhs.getReference())) {
							error("This is not a valid literal of the enum type: " + rhs.getReference().getName() + ".",
									ActionModelPackage.Literals.ASSIGNMENT_STATEMENT__RHS);
						}
					}
					else {
						error("The right hand side must be of type enumeration literal.", ActionModelPackage.Literals.ASSIGNMENT_STATEMENT__RHS);
					}
				}
			} catch (Exception exception) {
				// There is a type error on a lower level, no need to display the error message on this level too
			}
		}
		
		//Other checks
		if((assignment.getLhs() instanceof AccessExpression) && (assignment.getRhs() instanceof AccessExpression)) {
			error ("Assignment of the result of an access expression to the result of an access expression is currently not supported.", null);
		}
	}

	
	@Check
	public void CheckReturnStatementType(ReturnStatement rs) {
		ExpressionType returnStatementType = typeDeterminator.getType(rs.getExpression());
		
		ProcedureDeclaration containingProcedure = getContainingProcedure(rs);
		Type containingProcedureType = null;
		if(containingProcedure != null) {
			containingProcedureType = containingProcedure.getType();
		}
		if(!typeDeterminator.equals(containingProcedureType, returnStatementType)) {
			error("The type of the return statement (" + returnStatementType.toString().toLowerCase()
					+ ") does not match the declared type of the procedure (" 
					+ typeDeterminator.transform(containingProcedureType).toString().toLowerCase() + ").",
					null);	//Underlines the whole line
		}
	}
	
	@Check
	public void checkForStatement(ForStatement fs) {
		//TODO Parameter matches the range element type
		//TODO Range is a collection
		
	}
	
	//Access expression transformations are implemented as action transformations
	@Check
	public void checkAccessExpression(AccessExpression expression) {
		if (!(expression.eContainer() instanceof Action)) {
			error("This access expression is currently not supported. It is probably not contained in an action or it is part of a complex expression.",
					null);
		}
	}
	//Check function access expression redefined, because of the new element ProcedureDeclaration
	@Check
	public void checkFunctionAccessExpression(FunctionAccessExpression expression) {
		if (expression.getDeclaration() instanceof LambdaDeclaration) {
			error("Lambda declarations are currently not supported!",
					null);
		}
		else if (expression.getDeclaration() instanceof ProcedureDeclaration) {
			/*if(expression.eContainer() instanceof ValueDeclaration) {	//It should be guaranteed that it is in an action, otherwise it is implemented
				return;
			}*/
			if (expression.eContainer() instanceof SwitchStatement) {
				error("Function access expressions as control variables of switch statements are currently not supported.",
						null);
			}
			if (expression.eContainer() instanceof ForStatement) {
				error("Function access expressions as range variables of for statements are currently not supported.",
						null);
			}
		}
	}
	
	//TODO extract into util-class
	private ProcedureDeclaration getContainingProcedure(Action action) {
		EObject container = action.eContainer();
		if (container instanceof ProcedureDeclaration) {
			return (ProcedureDeclaration)container;
		} else if (container instanceof Branch) {
			return getContainingProcedure((Branch)container);
		} else if (container instanceof Block) {
			return getContainingProcedure((Action)container);
		} else if (container instanceof ForStatement) {
			return getContainingProcedure((Action)container);
		} else {
			return null;	//Not in a procedure
		}
	}
	
	//TODO extract into util-class
	private ProcedureDeclaration getContainingProcedure(Branch branch) {
		EObject container = branch.eContainer();
		if (container instanceof Action) {
			return getContainingProcedure((Action)container);
		} 
		throw new IllegalArgumentException("Unknown container for Branch.");
	}
}
