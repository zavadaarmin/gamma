/********************************************************************************
 * Copyright (c) 2018-2020 Contributors to the Gamma project
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package hu.bme.mit.gamma.expression.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

import hu.bme.mit.gamma.expression.derivedfeatures.ExpressionModelDerivedFeatures;
import hu.bme.mit.gamma.expression.model.AccessExpression;
import hu.bme.mit.gamma.expression.model.AddExpression;
import hu.bme.mit.gamma.expression.model.AndExpression;
import hu.bme.mit.gamma.expression.model.ArrayAccessExpression;
import hu.bme.mit.gamma.expression.model.ArrayLiteralExpression;
import hu.bme.mit.gamma.expression.model.ArrayTypeDefinition;
import hu.bme.mit.gamma.expression.model.BinaryExpression;
import hu.bme.mit.gamma.expression.model.BooleanTypeDefinition;
import hu.bme.mit.gamma.expression.model.ConstantDeclaration;
import hu.bme.mit.gamma.expression.model.DecimalLiteralExpression;
import hu.bme.mit.gamma.expression.model.DecimalTypeDefinition;
import hu.bme.mit.gamma.expression.model.Declaration;
import hu.bme.mit.gamma.expression.model.DirectReferenceExpression;
import hu.bme.mit.gamma.expression.model.EnumerationLiteralDefinition;
import hu.bme.mit.gamma.expression.model.EnumerationLiteralExpression;
import hu.bme.mit.gamma.expression.model.EnumerationTypeDefinition;
import hu.bme.mit.gamma.expression.model.EqualityExpression;
import hu.bme.mit.gamma.expression.model.Expression;
import hu.bme.mit.gamma.expression.model.ExpressionModelFactory;
import hu.bme.mit.gamma.expression.model.ExpressionPackage;
import hu.bme.mit.gamma.expression.model.FalseExpression;
import hu.bme.mit.gamma.expression.model.FieldAssignment;
import hu.bme.mit.gamma.expression.model.FieldDeclaration;
import hu.bme.mit.gamma.expression.model.FieldReferenceExpression;
import hu.bme.mit.gamma.expression.model.GreaterEqualExpression;
import hu.bme.mit.gamma.expression.model.GreaterExpression;
import hu.bme.mit.gamma.expression.model.IfThenElseExpression;
import hu.bme.mit.gamma.expression.model.InequalityExpression;
import hu.bme.mit.gamma.expression.model.InitializableElement;
import hu.bme.mit.gamma.expression.model.IntegerLiteralExpression;
import hu.bme.mit.gamma.expression.model.IntegerRangeLiteralExpression;
import hu.bme.mit.gamma.expression.model.IntegerTypeDefinition;
import hu.bme.mit.gamma.expression.model.LessEqualExpression;
import hu.bme.mit.gamma.expression.model.LessExpression;
import hu.bme.mit.gamma.expression.model.MultiaryExpression;
import hu.bme.mit.gamma.expression.model.MultiplyExpression;
import hu.bme.mit.gamma.expression.model.NotExpression;
import hu.bme.mit.gamma.expression.model.NullaryExpression;
import hu.bme.mit.gamma.expression.model.OrExpression;
import hu.bme.mit.gamma.expression.model.ParameterDeclaration;
import hu.bme.mit.gamma.expression.model.RationalLiteralExpression;
import hu.bme.mit.gamma.expression.model.RationalTypeDefinition;
import hu.bme.mit.gamma.expression.model.RecordAccessExpression;
import hu.bme.mit.gamma.expression.model.RecordLiteralExpression;
import hu.bme.mit.gamma.expression.model.RecordTypeDefinition;
import hu.bme.mit.gamma.expression.model.ReferenceExpression;
import hu.bme.mit.gamma.expression.model.SubtractExpression;
import hu.bme.mit.gamma.expression.model.TrueExpression;
import hu.bme.mit.gamma.expression.model.Type;
import hu.bme.mit.gamma.expression.model.TypeDeclaration;
import hu.bme.mit.gamma.expression.model.TypeDefinition;
import hu.bme.mit.gamma.expression.model.TypeReference;
import hu.bme.mit.gamma.expression.model.UnaryExpression;
import hu.bme.mit.gamma.expression.model.ValueDeclaration;
import hu.bme.mit.gamma.expression.model.VariableDeclaration;
import hu.bme.mit.gamma.expression.model.VariableDeclarationAnnotation;
import hu.bme.mit.gamma.util.GammaEcoreUtil;

public class ExpressionUtil {
	// Singleton
	public static final ExpressionUtil INSTANCE = new ExpressionUtil();
	protected ExpressionUtil() {}
	//
	
	protected final GammaEcoreUtil ecoreUtil = GammaEcoreUtil.INSTANCE;
	protected final ExpressionEvaluator evaluator = ExpressionEvaluator.INSTANCE;
	protected final ExpressionTypeDeterminator2 typeDeterminator = ExpressionTypeDeterminator2.INSTANCE;
	protected final ExpressionModelFactory factory = ExpressionModelFactory.eINSTANCE;
	
	// The following methods are worth extending in subclasses
	
	public Declaration getDeclaration(Expression expression) {
		if (expression instanceof DirectReferenceExpression) {
			DirectReferenceExpression reference = (DirectReferenceExpression) expression;
			return reference.getDeclaration();
		}
		if (expression instanceof RecordAccessExpression) {
			RecordAccessExpression access = (RecordAccessExpression) expression;
			FieldReferenceExpression reference = access.getFieldReference();
			return getDeclaration(reference);
		}
		if (expression instanceof FieldReferenceExpression) {
			FieldReferenceExpression reference = (FieldReferenceExpression) expression;
			return reference.getFieldDeclaration();
		}
		if (expression instanceof ArrayAccessExpression) {
			// ?
		}
		if (expression instanceof AccessExpression) {
			// Default access
			AccessExpression access = (AccessExpression) expression;
			Expression operand = access.getOperand();
			return getDeclaration(operand);
		}
		throw new IllegalArgumentException("Not known declaration: " + expression);
	}
	
	public ReferenceExpression getAccessReference(Expression expression) {
		if (expression instanceof DirectReferenceExpression) {
			return (DirectReferenceExpression) expression;
		}
		if (expression instanceof AccessExpression) {
			AccessExpression access = (AccessExpression) expression;
			return getAccessReference(access.getOperand());
		}
		// Could be extended to literals too
		throw new IllegalArgumentException("Not supported reference: " + expression);
	}
	
	public Declaration getAccessedDeclaration(Expression expression) {
		DirectReferenceExpression reference = (DirectReferenceExpression) getAccessReference(expression);
		return reference.getDeclaration();
	}
	
	public Collection<TypeDeclaration> getTypeDeclarations(EObject context) {
		ExpressionPackage _package = ecoreUtil.getSelfOrContainerOfType(context, ExpressionPackage.class);
		return _package.getTypeDeclarations();
	}
	
	//
	
	public IntegerRangeLiteralExpression getIntegerRangeLiteralExpression(Expression expression) {
		if (expression instanceof IntegerRangeLiteralExpression) {
			return (IntegerRangeLiteralExpression) expression;
		}
		if (expression instanceof DirectReferenceExpression) {
			DirectReferenceExpression reference = (DirectReferenceExpression) expression;
			Declaration declaration = reference.getDeclaration();
			if (declaration instanceof ConstantDeclaration) {
				ConstantDeclaration constant = (ConstantDeclaration) declaration;
				Expression value = constant.getExpression();
				if (value instanceof IntegerRangeLiteralExpression) {
					return (IntegerRangeLiteralExpression) value;
				}
			}
		}
		throw new IllegalArgumentException("Not known expression: " + expression);
	}
	
	// Expression optimization
	
	public Set<Expression> removeDuplicatedExpressions(Collection<Expression> expressions) {
		Set<Integer> integerValues = new HashSet<Integer>();
		Set<Boolean> booleanValues = new HashSet<Boolean>();
		Set<Expression> evaluatedExpressions = new HashSet<Expression>();
		for (Expression expression : expressions) {
			try {
				// Integers and enums
				int value = evaluator.evaluateInteger(expression);
				if (!integerValues.contains(value)) {
					integerValues.add(value);
					evaluatedExpressions.add(toIntegerLiteral(value));
				}
			} catch (Exception e) {}
			// Excluding branches
			try {
				// Boolean
				boolean bool = evaluator.evaluateBoolean(expression);
				if (!booleanValues.contains(bool)) {
					booleanValues.add(bool);
					evaluatedExpressions.add(bool ? factory.createTrueExpression() : factory.createFalseExpression());
				}
			} catch (Exception e) {}
		}
		return evaluatedExpressions;
	}
	
	public Collection<EnumerationLiteralExpression> mapToEnumerationLiterals(
			EnumerationTypeDefinition type, Collection<Expression> expressions) {
		List<EnumerationLiteralExpression> literals = new ArrayList<EnumerationLiteralExpression>();
		for (Expression expression : expressions) {
			int index = evaluator.evaluate(expression);
			EnumerationLiteralDefinition literal = type.getLiterals().get(index);
			EnumerationLiteralExpression literalExpression = createEnumerationLiteralExpression(literal);
			literals.add(literalExpression);
		}
		return literals;
	}

	public boolean isDefinitelyTrueExpression(Expression expression) {
		if (expression instanceof TrueExpression) {
			return true;
		}
		if (expression instanceof BinaryExpression) {
			BinaryExpression binaryExpression = (BinaryExpression) expression;
			Expression leftOperand = binaryExpression.getLeftOperand();
			Expression rightOperand = binaryExpression.getRightOperand();
			if (expression instanceof EqualityExpression ||
					expression instanceof GreaterEqualExpression ||
					expression instanceof LessEqualExpression) {
				if (ecoreUtil.helperEquals(leftOperand, rightOperand)) {
					return true;
				}
			}
			if (!(leftOperand instanceof EnumerationLiteralExpression
					&& rightOperand instanceof EnumerationLiteralExpression)) {
				// Different enum literals could be evaluated to the same value
				try {
					int leftValue = evaluator.evaluate(leftOperand);
					int rightValue = evaluator.evaluate(rightOperand);
					if (leftValue == rightValue) {
						if (expression instanceof EqualityExpression ||
								expression instanceof GreaterEqualExpression ||
								expression instanceof LessEqualExpression) {
							return true;
						}
					}
					else if (leftValue < rightValue) {
						if (expression instanceof LessExpression ||
								expression instanceof LessEqualExpression) {
							return true;
						}
					}
					else { // leftValue > rightValue
						if (expression instanceof GreaterExpression ||
								expression instanceof GreaterEqualExpression) {
							return true;
						}
					}
				} catch (IllegalArgumentException e) {
					// One of the arguments is not evaluable
				}
			}
		}
		if (expression instanceof NotExpression) {
			NotExpression notExpression = (NotExpression) expression;
			return isDefinitelyFalseExpression(notExpression.getOperand());
		}
		if (expression instanceof OrExpression) {
			OrExpression orExpression = (OrExpression) expression;
			for (Expression subExpression : orExpression.getOperands()) {
				if (isDefinitelyTrueExpression(subExpression)) {
					return true;
				}
			}
		}
		if (expression instanceof AndExpression) {
			AndExpression andExpression = (AndExpression) expression;
			for (Expression subExpression : andExpression.getOperands()) {
				if (!isDefinitelyTrueExpression(subExpression)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public boolean isDefinitelyFalseExpression(Expression expression) {
		if (expression instanceof FalseExpression) {
			return true;
		}
		// Checking 'Red == Green' kind of assumptions
		if (expression instanceof BinaryExpression) {
			BinaryExpression binaryExpression = (BinaryExpression) expression;
			Expression leftOperand = binaryExpression.getLeftOperand();
			Expression rightOperand = binaryExpression.getRightOperand();
			if (expression instanceof EqualityExpression) {
				if (leftOperand instanceof EnumerationLiteralExpression
						&& rightOperand instanceof EnumerationLiteralExpression) {
					EnumerationLiteralExpression lhs = (EnumerationLiteralExpression) leftOperand;
					EnumerationLiteralDefinition leftReference = lhs.getReference();
					EnumerationLiteralExpression rhs = (EnumerationLiteralExpression) rightOperand;
					EnumerationLiteralDefinition rightReference = rhs.getReference();
					if (!ecoreUtil.helperEquals(leftReference, rightReference)) {
						return true;
					}
				}
			}
			try {
				int leftValue = evaluator.evaluate(leftOperand);
				int rightValue = evaluator.evaluate(rightOperand);
				if (leftValue == rightValue) {
					if (expression instanceof InequalityExpression || 
							expression instanceof LessExpression ||
							expression instanceof GreaterExpression) {
						return true;
					}
				}
				else { // leftValue != rightValue
					if (expression instanceof EqualityExpression) {
						return true;
					}
					if (leftValue < rightValue) {
						if (expression instanceof GreaterExpression ||
								expression instanceof GreaterEqualExpression) {
							return true;
						}
					}
					else { // leftValue > rightValue
						if (expression instanceof LessExpression ||
								expression instanceof LessEqualExpression) {
							return true;
						}
					}
				}
			} catch (IllegalArgumentException e) {
				// One of the arguments is not evaluable
			}
		}
		if (expression instanceof NotExpression) {
			NotExpression notExpression = (NotExpression) expression;
			return isDefinitelyTrueExpression(notExpression.getOperand());
		}
		if (expression instanceof AndExpression) {
			AndExpression andExpression = (AndExpression) expression;
			for (Expression subExpression : andExpression.getOperands()) {
				if (isDefinitelyFalseExpression(subExpression)) {
					return true;
				}
			}
			Collection<EqualityExpression> allEqualityExpressions = collectAllEqualityExpressions(andExpression);
			List<EqualityExpression> referenceEqualityExpressions = filterReferenceEqualityExpressions(
					allEqualityExpressions);
			if (hasEqualityToDifferentLiterals(referenceEqualityExpressions)) {
				return true;
			}
		}
		if (expression instanceof OrExpression) {
			OrExpression orExpression = (OrExpression) expression;
			for (Expression subExpression : orExpression.getOperands()) {
				if (!isDefinitelyFalseExpression(subExpression)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns whether the disjunction of the given expressions is a certain event.
	 */
	public boolean isCertainEvent(Expression lhs, Expression rhs) {
		if (lhs instanceof NotExpression) {
			NotExpression notExpression = (NotExpression) lhs;
			final Expression operand = notExpression.getOperand();
			if (ecoreUtil.helperEquals(operand, rhs)) {
				return true;
			}
		}
		if (rhs instanceof NotExpression) {
			NotExpression notExpression = (NotExpression) rhs;
			final Expression operand = notExpression.getOperand();
			if (ecoreUtil.helperEquals(operand, lhs)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean hasEqualityToDifferentLiterals(List<EqualityExpression> expressions) {
		for (int i = 0; i < expressions.size() - 1; ++i) {
			try {
				EqualityExpression leftEqualityExpression = expressions.get(i);
				Entry<Declaration, Expression> left = getDeclarationExpressions(leftEqualityExpression);
				Declaration leftDeclaration = left.getKey();
				Expression leftValueExpression = left.getValue();
				int leftValue = evaluator.evaluate(leftValueExpression);
				for (int j = i + 1; j < expressions.size(); ++j) {
					try {
						EqualityExpression rightEqualityExpression = expressions.get(j);
						Entry<Declaration, Expression> right = getDeclarationExpressions(rightEqualityExpression);
						Declaration rightDeclaration = right.getKey();
						if (leftDeclaration == rightDeclaration) {
							Expression rightValueExpression = right.getValue();
							int rightValue = evaluator.evaluate(rightValueExpression);
							if (leftValue != rightValue) {
								return true;
							}
						}
					} catch (IllegalArgumentException e) {
						// j is not evaluable
						expressions.remove(j);
						--j;
					}
				}
			} catch (IllegalArgumentException e) {
				// i is not evaluable
				expressions.remove(i);
				--i;
			}
		}
		return false;
	}
	
	protected Entry<Declaration, Expression> getDeclarationExpressions(BinaryExpression expression) {
		Expression leftOperand = expression.getLeftOperand();
		Declaration declaration = getDeclaration(leftOperand);
		Expression rightOperand = expression.getRightOperand();
		return new SimpleEntry<Declaration, Expression>(declaration, rightOperand);
	}
	
	public Collection<EqualityExpression> collectAllEqualityExpressions(AndExpression expression) {
		List<EqualityExpression> equalityExpressions = new ArrayList<EqualityExpression>();
		for (Expression subexpression : expression.getOperands()) {
			if (subexpression instanceof EqualityExpression) {
				EqualityExpression equalityExpression = (EqualityExpression) subexpression;
				equalityExpressions.add(equalityExpression);
			}
			else if (subexpression instanceof AndExpression) {
				AndExpression andExpression = (AndExpression) subexpression;
				equalityExpressions.addAll(collectAllEqualityExpressions(andExpression));
			}
		}
		return equalityExpressions;
	}

	public List<EqualityExpression> filterReferenceEqualityExpressions(
			Collection<EqualityExpression> expressions) {
		return expressions.stream().filter(
				it -> it.getLeftOperand() instanceof ReferenceExpression
				&& !(it.getRightOperand() instanceof ReferenceExpression))
			.collect(Collectors.toList());
	}

	// Arithmetic: for now, integers only

	public Expression add(Expression expression, int value) {
		return toIntegerLiteral(evaluator.evaluate(expression) + value);
	}

	public Expression subtract(Expression expression, int value) {
		return toIntegerLiteral(evaluator.evaluate(expression) - value);
	}
	
	public Expression createIncrementExpression(VariableDeclaration variable) {
		return wrapIntoAdd(createReferenceExpression(variable), 1);
	}

	public Expression createDecrementExpression(VariableDeclaration variable) {
		return wrapIntoSubtract(createReferenceExpression(variable), 1);
	}
	
	public Expression wrapIntoAdd(Expression expression, int value) {
		AddExpression addExpression = factory.createAddExpression();
		addExpression.getOperands().add(expression);
		addExpression.getOperands().add(toIntegerLiteral(value));
		return addExpression;
	}
	
	public Expression wrapIntoSubtract(Expression expression, int value) {
		SubtractExpression subtractExpression = factory.createSubtractExpression();
		subtractExpression.setLeftOperand(expression);
		subtractExpression.setRightOperand(toIntegerLiteral(value));
		return subtractExpression;
	}
	
	public Expression wrapIntoMultiply(Expression expression, int value) {
		MultiplyExpression multiplyExpression = factory.createMultiplyExpression();
		multiplyExpression.getOperands().add(expression);
		multiplyExpression.getOperands().add(toIntegerLiteral(value));
		return multiplyExpression;
	}

	// Declaration references
	
	// Variables
	public Set<VariableDeclaration> getReferredVariables(EObject object) {
		Set<VariableDeclaration> variables = new HashSet<VariableDeclaration>();
		for (DirectReferenceExpression referenceExpression :
				ecoreUtil.getSelfAndAllContentsOfType(object, DirectReferenceExpression.class)) {
			Declaration declaration = referenceExpression.getDeclaration();
			if (declaration instanceof VariableDeclaration) {
				variables.add((VariableDeclaration) declaration);
			}
		}
		return variables;
	}
	
	protected Set<VariableDeclaration> _getReferredVariables(NullaryExpression expression) {
		return Collections.emptySet();
	}

	protected Set<VariableDeclaration> _getReferredVariables(UnaryExpression expression) {
		return getReferredVariables(expression.getOperand());
	}

	protected Set<VariableDeclaration> _getReferredVariables(IfThenElseExpression expression) {
		Set<VariableDeclaration> variables = new HashSet<VariableDeclaration>();
		variables.addAll(getReferredVariables(expression.getCondition()));
		variables.addAll(getReferredVariables(expression.getThen()));
		variables.addAll(getReferredVariables(expression.getElse()));
		return variables;
	}

	protected Set<VariableDeclaration> _getReferredVariables(ReferenceExpression expression) {
		if (expression instanceof DirectReferenceExpression) {
			DirectReferenceExpression directReferenceExpression = (DirectReferenceExpression) expression;
			Declaration declaration = directReferenceExpression.getDeclaration();
			if (declaration instanceof VariableDeclaration) {
				return Collections.singleton((VariableDeclaration) declaration);
			}
		} else if (expression instanceof ArrayAccessExpression) {
			ArrayAccessExpression arrayAccessExpression = (ArrayAccessExpression) expression;
			Set<VariableDeclaration> variables = new HashSet<VariableDeclaration>();
			variables.addAll(getReferredVariables(arrayAccessExpression.getOperand()));
			variables.addAll(getReferredVariables(arrayAccessExpression.getIndex()));
			return variables;
		} else if (expression instanceof AccessExpression) {
			AccessExpression accessExpression = (AccessExpression) expression;
			return getReferredVariables(accessExpression.getOperand());
		}
		return Collections.emptySet();
	}

	protected Set<VariableDeclaration> _getReferredVariables(BinaryExpression expression) {
		Set<VariableDeclaration> variables = new HashSet<VariableDeclaration>();
		variables.addAll(getReferredVariables(expression.getLeftOperand()));
		variables.addAll(getReferredVariables(expression.getRightOperand()));
		return variables;
	}

	protected Set<VariableDeclaration> _getReferredVariables(MultiaryExpression expression) {
		Set<VariableDeclaration> variables = new HashSet<VariableDeclaration>();
		EList<Expression> _operands = expression.getOperands();
		for (Expression operand : _operands) {
			variables.addAll(getReferredVariables(operand));
		}
		return variables;
	}

	public Set<VariableDeclaration> getReferredVariables(Expression expression) {
		if (expression instanceof ReferenceExpression) {
			return _getReferredVariables((ReferenceExpression) expression);
		} else if (expression instanceof BinaryExpression) {
			return _getReferredVariables((BinaryExpression) expression);
		} else if (expression instanceof IfThenElseExpression) {
			return _getReferredVariables((IfThenElseExpression) expression);
		} else if (expression instanceof MultiaryExpression) {
			return _getReferredVariables((MultiaryExpression) expression);
		} else if (expression instanceof NullaryExpression) {
			return _getReferredVariables((NullaryExpression) expression);
		} else if (expression instanceof UnaryExpression) {
			return _getReferredVariables((UnaryExpression) expression);
		} else {
			throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.<Object>asList(expression).toString());
		}
	}
	
	// Parameters
	public Set<ParameterDeclaration> getReferredParameters(EObject object) {
		Set<ParameterDeclaration> parameters = new HashSet<ParameterDeclaration>();
		for (DirectReferenceExpression referenceExpression :
				ecoreUtil.getSelfAndAllContentsOfType(object, DirectReferenceExpression.class)) {
			Declaration declaration = referenceExpression.getDeclaration();
			if (declaration instanceof ParameterDeclaration) {
				parameters.add((ParameterDeclaration) declaration);
			}
		}
		return parameters;
	}
	
	protected Set<ParameterDeclaration> _getReferredParameters(NullaryExpression expression) {
		return Collections.emptySet();
	}

	protected Set<ParameterDeclaration> _getReferredParameters(UnaryExpression expression) {
		return getReferredParameters(expression.getOperand());
	}

	protected Set<ParameterDeclaration> _getReferredParameters(IfThenElseExpression expression) {
		Set<ParameterDeclaration> parameters = new HashSet<ParameterDeclaration>();
		parameters.addAll(getReferredParameters(expression.getCondition()));
		parameters.addAll(getReferredParameters(expression.getThen()));
		parameters.addAll(getReferredParameters(expression.getElse()));
		return parameters;
	}

	protected Set<ParameterDeclaration> _getReferredParameters(ReferenceExpression expression) {
		if (expression instanceof DirectReferenceExpression) {
			DirectReferenceExpression reference = (DirectReferenceExpression) expression;
			Declaration declaration = reference.getDeclaration();
			if (declaration instanceof ParameterDeclaration) {
				ParameterDeclaration parameter = (ParameterDeclaration) declaration;
				return Collections.singleton(parameter);
			}
		}
		else if (expression instanceof AccessExpression) {
			AccessExpression accessExpression = (AccessExpression) expression;
			return getReferredParameters(accessExpression.getOperand());
		}
		return Collections.emptySet();
	}

	protected Set<ParameterDeclaration> _getReferredParameters(BinaryExpression expression) {
		Set<ParameterDeclaration> parameters = new HashSet<ParameterDeclaration>();
		parameters.addAll(getReferredParameters(expression.getLeftOperand()));
		parameters.addAll(getReferredParameters(expression.getRightOperand()));
		return parameters;
	}

	protected Set<ParameterDeclaration> _getReferredParameters(MultiaryExpression expression) {
		Set<ParameterDeclaration> parameters = new HashSet<ParameterDeclaration>();
		EList<Expression> _operands = expression.getOperands();
		for (Expression operand : _operands) {
			parameters.addAll(getReferredParameters(operand));
		}
		return parameters;
	}

	public Set<ParameterDeclaration> getReferredParameters(Expression expression) {
		if (expression instanceof ReferenceExpression) {
			return _getReferredParameters((ReferenceExpression) expression);
		} else if (expression instanceof BinaryExpression) {
			return _getReferredParameters((BinaryExpression) expression);
		} else if (expression instanceof IfThenElseExpression) {
			return _getReferredParameters((IfThenElseExpression) expression);
		} else if (expression instanceof MultiaryExpression) {
			return _getReferredParameters((MultiaryExpression) expression);
		} else if (expression instanceof NullaryExpression) {
			return _getReferredParameters((NullaryExpression) expression);
		} else if (expression instanceof UnaryExpression) {
			return _getReferredParameters((UnaryExpression) expression);
		} else {
			throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.<Object>asList(expression).toString());
		}
	}
	
	// Constants
	public Set<ConstantDeclaration> getReferredConstants(EObject object) {
		Set<ConstantDeclaration> constants = new HashSet<ConstantDeclaration>();
		for (DirectReferenceExpression referenceExpression :
				ecoreUtil.getSelfAndAllContentsOfType(object, DirectReferenceExpression.class)) {
			Declaration declaration = referenceExpression.getDeclaration();
			if (declaration instanceof ConstantDeclaration) {
				constants.add((ConstantDeclaration) declaration);
			}
		}
		return constants;
	}
	
	protected Set<ConstantDeclaration> _getReferredConstants(NullaryExpression expression) {
		return Collections.emptySet();
	}

	protected Set<ConstantDeclaration> _getReferredConstants(UnaryExpression expression) {
		return getReferredConstants(expression.getOperand());
	}

	protected Set<ConstantDeclaration> _getReferredConstants(IfThenElseExpression expression) {
		Set<ConstantDeclaration> constants = new HashSet<ConstantDeclaration>();
		constants.addAll(getReferredConstants(expression.getCondition()));
		constants.addAll(getReferredConstants(expression.getThen()));
		constants.addAll(getReferredConstants(expression.getElse()));
		return constants;
	}

	protected Set<ConstantDeclaration> _getReferredConstants(ReferenceExpression expression) {
		if (expression instanceof DirectReferenceExpression ) {
			DirectReferenceExpression reference = (DirectReferenceExpression) expression;
			Declaration declaration = reference.getDeclaration();
			if (declaration instanceof ConstantDeclaration) {
				ConstantDeclaration constant = (ConstantDeclaration) declaration;
				return Collections.singleton(constant);
			}
		}
		else if (expression instanceof AccessExpression) {
			AccessExpression accessExpression = (AccessExpression) expression;
			return getReferredConstants(accessExpression.getOperand());
		}
		return Collections.emptySet();
	}

	protected Set<ConstantDeclaration> _getReferredConstants(BinaryExpression expression) {
		Set<ConstantDeclaration> constants = new HashSet<ConstantDeclaration>();
		constants.addAll(getReferredConstants(expression.getLeftOperand()));
		constants.addAll(getReferredConstants(expression.getRightOperand()));
		return constants;
	}

	protected Set<ConstantDeclaration> _getReferredConstants(MultiaryExpression expression) {
		Set<ConstantDeclaration> constants = new HashSet<ConstantDeclaration>();
		EList<Expression> _operands = expression.getOperands();
		for (Expression operand : _operands) {
			constants.addAll(getReferredConstants(operand));
		}
		return constants;
	}

	public Set<ConstantDeclaration> _getReferredConstants(Expression expression) {
		if (expression instanceof ReferenceExpression) {
			return _getReferredConstants((ReferenceExpression) expression);
		} else if (expression instanceof BinaryExpression) {
			return _getReferredConstants((BinaryExpression) expression);
		} else if (expression instanceof IfThenElseExpression) {
			return _getReferredConstants((IfThenElseExpression) expression);
		} else if (expression instanceof MultiaryExpression) {
			return _getReferredConstants((MultiaryExpression) expression);
		} else if (expression instanceof NullaryExpression) {
			return _getReferredConstants((NullaryExpression) expression);
		} else if (expression instanceof UnaryExpression) {
			return _getReferredConstants((UnaryExpression) expression);
		} else {
			throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.<Object>asList(expression).toString());
		}
	}
	
	public Set<ConstantDeclaration> getReferredConstants(Expression expression) {
		if (expression instanceof ReferenceExpression) {
			return _getReferredConstants((ReferenceExpression) expression);
		} else if (expression instanceof BinaryExpression) {
			return _getReferredConstants((BinaryExpression) expression);
		} else if (expression instanceof IfThenElseExpression) {
			return _getReferredConstants((IfThenElseExpression) expression);
		} else if (expression instanceof MultiaryExpression) {
			return _getReferredConstants((MultiaryExpression) expression);
		} else if (expression instanceof NullaryExpression) {
			return _getReferredConstants((NullaryExpression) expression);
		} else if (expression instanceof UnaryExpression) {
			return _getReferredConstants((UnaryExpression) expression);
		} else {
			throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.<Object>asList(expression).toString());
		}
	}
	
	// Values (variables, parameters and constants)
	
	public Set<ValueDeclaration> getReferredValues(Expression expression) {
		Set<ValueDeclaration> referred = new HashSet<ValueDeclaration>();
		referred.addAll(getReferredVariables(expression));
		referred.addAll(getReferredParameters(expression));
		referred.addAll(getReferredConstants(expression));
		return referred;
	}
	
	// Extract parameters
	
	public List<ConstantDeclaration> extractParamaters(
			List<? extends ParameterDeclaration> parameters, List<String> names,
			List<? extends Expression> arguments) {
		List<ConstantDeclaration> constants = new ArrayList<ConstantDeclaration>();
		int size = parameters.size();
		for (int i = 0; i < size; i++) {
			ParameterDeclaration parameter = parameters.get(i);
			Type type = ecoreUtil.clone(parameter.getType());
			String name = names.get(i);
			Expression value = ecoreUtil.clone(arguments.get(i));
			ConstantDeclaration constant = factory.createConstantDeclaration();
			constant.setName(name);
			constant.setType(type);
			constant.setExpression(value);
			constants.add(constant);
			// Changing the references to the constant
			ecoreUtil.change(constant, parameter, parameter.eContainer());
		}
		return constants;
	}
	
	// Initial values of types

	public Expression getInitialValue(VariableDeclaration variableDeclaration) {
		final Expression initialValue = variableDeclaration.getExpression();
		if (initialValue != null) {
			return ecoreUtil.clone(initialValue);
		}
		final Type type = variableDeclaration.getType();
		return getInitialValueOfType(type);
	}
	
	protected Expression _getInitialValueOfType(TypeReference type) {
		return getInitialValueOfType(type.getReference().getType());
	}

	protected Expression _getInitialValueOfType(BooleanTypeDefinition type) {
		return factory.createFalseExpression();
	}

	protected Expression _getInitialValueOfType(IntegerTypeDefinition type) {
		return toIntegerLiteral(0);
	}

	protected Expression _getInitialValueOfType(DecimalTypeDefinition type) {
		DecimalLiteralExpression decimalLiteralExpression = factory.createDecimalLiteralExpression();
		decimalLiteralExpression.setValue(BigDecimal.ZERO);
		return decimalLiteralExpression;
	}

	protected Expression _getInitialValueOfType(RationalTypeDefinition type) {
		RationalLiteralExpression rationalLiteralExpression = factory.createRationalLiteralExpression();
		rationalLiteralExpression.setNumerator(BigInteger.ZERO);
		rationalLiteralExpression.setDenominator(BigInteger.ONE);
		return rationalLiteralExpression;
	}

	protected Expression _getInitialValueOfType(EnumerationTypeDefinition type) {
		EnumerationLiteralDefinition literal = type.getLiterals().get(0);
		return createEnumerationLiteralExpression(literal);
	}
	
	protected Expression _getInitialValueOfType(ArrayTypeDefinition type) {
		ArrayLiteralExpression arrayLiteralExpression = factory.createArrayLiteralExpression();
		int arraySize = evaluator.evaluateInteger(type.getSize());
		for (int i = 0; i < arraySize; ++i) {
			Expression elementDefaultValue = getInitialValueOfType(type.getElementType());
			arrayLiteralExpression.getOperands().add(elementDefaultValue);
		}
		return arrayLiteralExpression;
	}
	
	protected Expression _getInitialValueOfType(RecordTypeDefinition type) {
		TypeDeclaration typeDeclaration = ecoreUtil.getContainerOfType(type, TypeDeclaration.class);
		if (typeDeclaration == null) {
			throw new IllegalArgumentException("Record type is not contained by declaration: " + type);
		}
		RecordLiteralExpression recordLiteralExpression = factory.createRecordLiteralExpression();
		recordLiteralExpression.setTypeDeclaration(typeDeclaration);
		for (FieldDeclaration field : type.getFieldDeclarations()) {
			FieldAssignment assignment = factory.createFieldAssignment();
			FieldReferenceExpression fieldReference = factory.createFieldReferenceExpression();
			fieldReference.setFieldDeclaration(field);
			assignment.setReference(fieldReference);
			assignment.setValue(getInitialValueOfType(field.getType()));
			recordLiteralExpression.getFieldAssignments().add(assignment);
		}
		return recordLiteralExpression;
	}

	public Expression getInitialValueOfType(Type type) {
		if (type instanceof EnumerationTypeDefinition) {
			return _getInitialValueOfType((EnumerationTypeDefinition) type);
		} else if (type instanceof DecimalTypeDefinition) {
			return _getInitialValueOfType((DecimalTypeDefinition) type);
		} else if (type instanceof IntegerTypeDefinition) {
			return _getInitialValueOfType((IntegerTypeDefinition) type);
		} else if (type instanceof RationalTypeDefinition) {
			return _getInitialValueOfType((RationalTypeDefinition) type);
		} else if (type instanceof BooleanTypeDefinition) {
			return _getInitialValueOfType((BooleanTypeDefinition) type);
		} else if (type instanceof TypeReference) {
			return _getInitialValueOfType((TypeReference) type);
		} else if (type instanceof ArrayTypeDefinition) {
			return _getInitialValueOfType((ArrayTypeDefinition) type);
		} else if (type instanceof RecordTypeDefinition) {
			return _getInitialValueOfType((RecordTypeDefinition) type);
		} else {
			throw new IllegalArgumentException("Unhandled parameter types: " + type);
		}
	}
	
	//
	
	public TypeReference createTypeReference(TypeDeclaration type) {
		TypeReference typeReference = factory.createTypeReference();
		typeReference.setReference(type);
		return typeReference;
	}
	
	// Variable handling
	
	public TypeDeclaration wrapIntoDeclaration(Type type, String name) {
		TypeDeclaration declaration = factory.createTypeDeclaration();
		declaration.setName(name);
		declaration.setType(type);
		return declaration;
	}
	
	public AndExpression connectThroughNegations(VariableDeclaration ponate,
			Iterable<? extends ValueDeclaration> toBeNegated) {
		AndExpression and = connectThroughNegations(toBeNegated);
		DirectReferenceExpression ponateReference = factory.createDirectReferenceExpression();
		ponateReference.setDeclaration(ponate);
		and.getOperands().add(ponateReference);
		return and;
	}
	
	public AndExpression connectThroughNegations(Iterable<? extends ValueDeclaration> toBeNegated) {
		Collection<DirectReferenceExpression> toBeNegatedReferences = new ArrayList<DirectReferenceExpression>();
		for (ValueDeclaration toBeNegatedVariable : toBeNegated) {
			DirectReferenceExpression reference = factory.createDirectReferenceExpression();
			reference.setDeclaration(toBeNegatedVariable);
			toBeNegatedReferences.add(reference);
		}
		return connectViaNegations(toBeNegatedReferences);
	}
	
	public AndExpression connectViaNegations(Iterable<? extends Expression> toBeNegated) {
		AndExpression and = factory.createAndExpression();
		List<Expression> operands = and.getOperands();
		for (Expression expression : toBeNegated) {
			NotExpression not = factory.createNotExpression();
			not.setOperand(expression);
			operands.add(not);
		}
		if (operands.isEmpty()) {
			// If collection is empty, the expression is always true
			operands.add(factory.createTrueExpression());
		}
		return and;
	}
	
	public void reduceCrossReferenceChain(
			Iterable<? extends InitializableElement> initializableElements, EObject context) {
		for (InitializableElement element : initializableElements) {
			Expression initialExpression = element.getExpression();
			if (initialExpression instanceof DirectReferenceExpression) {
				DirectReferenceExpression reference = (DirectReferenceExpression) initialExpression;
				Declaration referencedDeclaration = reference.getDeclaration();
				ecoreUtil.change(referencedDeclaration, element, context);
			}
		}
	}
	
	// Variable annotation handling
	
	public void addTransientAnnotation(VariableDeclaration variable) {
		addAnnotation(variable, factory.createTransientVariableDeclarationAnnotation());
	}
	
	public void addResettableAnnotation(VariableDeclaration variable) {
		addAnnotation(variable, factory.createResettableVariableDeclarationAnnotation());
	}
	
	public void addEnvironmentResettableAnnotation(VariableDeclaration variable) {
		addAnnotation(variable, factory.createEnvironmentResettableVariableDeclarationAnnotation());
	}
	
	public void addClockAnnotation(VariableDeclaration variable) {
		addAnnotation(variable, factory.createClockVariableDeclarationAnnotation());
	}
	
	public void addAnnotation(VariableDeclaration variable, VariableDeclarationAnnotation annotation) {
		if (variable != null) {
			variable.getAnnotations().add(annotation);
		}
	}
	
	public void removeVariableDeclarationAnnotations(
			Collection<? extends VariableDeclaration> variables,
			Class<? extends VariableDeclarationAnnotation> annotationClass) {
		for (VariableDeclaration variable : variables) {
			List<VariableDeclarationAnnotation> annotations =
					new ArrayList<VariableDeclarationAnnotation>(variable.getAnnotations());
			for (VariableDeclarationAnnotation annotation : annotations) {
				if (annotationClass.isInstance(annotation)) {
					ecoreUtil.remove(annotation);
				}
			}
		}
	}
	
	// Creators
	
	public BigInteger toBigInt(long value) {
		return BigInteger.valueOf(value);
	}
	
	public IntegerLiteralExpression toIntegerLiteral(long value) {
		return toIntegerLiteral(toBigInt(value));
	}
	
	public IntegerLiteralExpression toIntegerLiteral(BigInteger value) {
		IntegerLiteralExpression integerLiteral = factory.createIntegerLiteralExpression();
		integerLiteral.setValue(value);
		return integerLiteral;
	}
	
	public VariableDeclaration createVariableDeclaration(Type type, String name) {
		return createVariableDeclaration(type, name, null);
	}
	
	public VariableDeclaration createVariableDeclarationWithDefaultInitialValue(
			Type type, String name) {
		return createVariableDeclaration(type, name,
				ExpressionModelDerivedFeatures.getDefaultExpression(type));
	}
	
	public VariableDeclaration createVariableDeclaration(Type type, String name, Expression expression) {
		VariableDeclaration variableDeclaration = factory.createVariableDeclaration();
		variableDeclaration.setType(type);
		variableDeclaration.setName(name);
		variableDeclaration.setExpression(expression);
		return variableDeclaration;
	}
	
	public IntegerRangeLiteralExpression createIntegerRangeLiteralExpression(
			Expression start, boolean leftInclusive, Expression end, boolean rightIclusive) {
		IntegerRangeLiteralExpression range = factory.createIntegerRangeLiteralExpression();
		range.setLeftOperand(start);
		range.setLeftInclusive(leftInclusive);
		range.setRightOperand(end);
		range.setRightInclusive(rightIclusive);
		return range;
	}

	public ParameterDeclaration createParameterDeclaration(Type type, String name) {
		ParameterDeclaration parameterDeclaration = factory.createParameterDeclaration();
		parameterDeclaration.setType(type);
		parameterDeclaration.setName(name);
		return parameterDeclaration;
	}
	
	public NotExpression createNotExpression(Expression expression) {
		NotExpression notExpression = factory.createNotExpression();
		notExpression.setOperand(expression);
		return notExpression;
	}
	
	public IfThenElseExpression createIfThenElseExpression(Expression _if,
			Expression then, Expression _else) {
		IfThenElseExpression ifThenElseExpression = factory.createIfThenElseExpression();
		ifThenElseExpression.setCondition(_if);
		ifThenElseExpression.setThen(then);
		ifThenElseExpression.setElse(_else);
		return ifThenElseExpression;
	}
	
	public DirectReferenceExpression createReferenceExpression(ValueDeclaration variable) {
		DirectReferenceExpression reference = factory.createDirectReferenceExpression();
		reference.setDeclaration(variable);
		return reference;
	}
	
	public EqualityExpression createEqualityExpression(VariableDeclaration variable, Expression expression) {
		EqualityExpression equalityExpression = factory.createEqualityExpression();
		equalityExpression.setLeftOperand(createReferenceExpression(variable));
		equalityExpression.setRightOperand(expression);
		return equalityExpression;
	}
	
	public EqualityExpression createEqualityExpression(Expression lhs, Expression rhs) {
		EqualityExpression equalityExpression = factory.createEqualityExpression();
		equalityExpression.setLeftOperand(lhs);
		equalityExpression.setRightOperand(rhs);
		return equalityExpression;
	}
	
	public InequalityExpression createInequalityExpression(VariableDeclaration variable, Expression expression) {
		InequalityExpression inequalityExpression = factory.createInequalityExpression();
		inequalityExpression.setLeftOperand(createReferenceExpression(variable));
		inequalityExpression.setRightOperand(expression);
		return inequalityExpression;
	}
	
	public InequalityExpression createInequalityExpression(Expression lhs, Expression rhs) {
		InequalityExpression inequalityExpression = factory.createInequalityExpression();
		inequalityExpression.setLeftOperand(lhs);
		inequalityExpression.setRightOperand(rhs);
		return inequalityExpression;
	}
	
	public LessExpression createLessExpression(Expression lhs, Expression rhs) {
		LessExpression lessExpression = factory.createLessExpression();
		lessExpression.setLeftOperand(lhs);
		lessExpression.setRightOperand(rhs);
		return lessExpression;
	}
	
	public IfThenElseExpression createMinExpression(Expression lhs, Expression rhs) {
		return createIfThenElseExpression(createLessExpression(lhs, rhs),
				ecoreUtil.clone(lhs), ecoreUtil.clone(rhs));
	}
	
	public IfThenElseExpression createMaxExpression(Expression lhs, Expression rhs) {
		return createIfThenElseExpression(createLessExpression(lhs, rhs),
				ecoreUtil.clone(rhs), ecoreUtil.clone(lhs));
	}
	
	public EnumerationLiteralExpression createEnumerationLiteralExpression(
			EnumerationLiteralDefinition literal) {
		EnumerationLiteralExpression literalExpression = factory.createEnumerationLiteralExpression();
		literalExpression.setReference(literal);
		TypeDeclaration typeDeclaration = ExpressionModelDerivedFeatures.getTypeDeclaration(literal);
		TypeReference typeReference = createTypeReference(typeDeclaration);
		literalExpression.setTypeReference(typeReference);
		return literalExpression;
	}
	
	public Expression replaceAndWrapIntoMultiaryExpression(Expression original,
			Expression addition, MultiaryExpression potentialContainer) {
		if (original == null && addition == null) {
			throw new IllegalArgumentException("Null original or addition parameter: " + original + " " + addition);
		}
		ecoreUtil.replace(potentialContainer, original);
		return wrapIntoMultiaryExpression(original, addition, potentialContainer);
	}
	
	public Expression wrapIntoMultiaryExpression(Expression original,
			Expression addition, MultiaryExpression potentialContainer) {
		if (original == null) {
			return addition;
		}
		if (addition == null) {
			return original;
		}
		List<Expression> operands = potentialContainer.getOperands();
		operands.add(original);
		operands.add(addition);
		return potentialContainer;
	}
	
	public Expression wrapIntoMultiaryExpression(Expression original,
			Collection<? extends Expression> additions, MultiaryExpression potentialContainer) {
		List<Expression> operands = potentialContainer.getOperands();
		if (original != null) {
			operands.add(original);
		}
		operands.addAll(additions);
		operands.removeIf(it -> it == null);
		if (operands.isEmpty()) {
			return null;
		}
		if (operands.size() == 1) {
			return operands.get(0);
		}
		return potentialContainer;
	}
	
	public Expression wrapIntoMultiaryExpression(Collection<? extends Expression> expressions,
			MultiaryExpression potentialContainer) {
		if (expressions.isEmpty()) {
			return null;
		}
		int size = expressions.size();
		if (size == 1) {
			return expressions.iterator().next();
		}
		potentialContainer.getOperands().addAll(expressions);
		return potentialContainer;
	}
	
	public Expression wrapIntoOrExpression(Collection<? extends Expression> expressions) {
		return wrapIntoMultiaryExpression(expressions, factory.createOrExpression());
	}
	
	public ReferenceExpression index(ValueDeclaration declaration, List<Expression> indexes) {
		if (indexes.isEmpty()) {
			return createReferenceExpression(declaration);
		}
		int index = indexes.size() - 1;
		Expression lastIndex = indexes.get(index);
		ArrayAccessExpression access = factory.createArrayAccessExpression();
		access.setOperand(index(declaration, indexes.subList(0, index)));
		access.setIndex(lastIndex);
		return access;
	}
	
	public MultiaryExpression cloneIntoMultiaryExpression(Expression expression,
			MultiaryExpression container) {
		ecoreUtil.replace(container, expression);
		container.getOperands().add(expression);
		container.getOperands().add(ecoreUtil.clone(expression));
		return container;
	}
	
	// Unwrapper
	
	public Expression unwrapIfPossible(Expression expression) {
		if (expression instanceof MultiaryExpression) {
			MultiaryExpression multiaryExpression = (MultiaryExpression) expression;
			List<Expression> operands = multiaryExpression.getOperands();
			int size = operands.size();
			if (size >= 2) {
				return multiaryExpression;
			}
			if (size == 1) {
				return unwrapIfPossible(operands.get(0));
			}
			else {
				throw new IllegalStateException("Empty expression" + expression + " " + size);
			}
		}
		return expression;
	}
	
	// Message queue - array handling
	 
	public Expression peek(VariableDeclaration queue) {
		TypeDefinition typeDefinition = ExpressionModelDerivedFeatures.getTypeDefinition(queue);
		if (typeDefinition instanceof ArrayTypeDefinition) {
			ArrayAccessExpression accessExpression = factory.createArrayAccessExpression();
			accessExpression.setOperand(createReferenceExpression(queue));
			accessExpression.setIndex(toIntegerLiteral(0));
			return accessExpression;
		}
		throw new IllegalArgumentException("Not an array: " + queue);
	}
	
}