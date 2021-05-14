/********************************************************************************
 * Copyright (c) 2020-2021 Contributors to the Gamma project
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package hu.bme.mit.gamma.scenario.language.validation

import hu.bme.mit.gamma.expression.model.Expression
import hu.bme.mit.gamma.expression.model.ExpressionModelPackage
import hu.bme.mit.gamma.expression.util.ExpressionEvaluator
import hu.bme.mit.gamma.scenario.language.util.StatechartLanguageUtil
import hu.bme.mit.gamma.scenario.model.Chart
import hu.bme.mit.gamma.scenario.model.CombinedFragment
import hu.bme.mit.gamma.scenario.model.Delay
import hu.bme.mit.gamma.scenario.model.Interaction
import hu.bme.mit.gamma.scenario.model.InteractionDirection
import hu.bme.mit.gamma.scenario.model.InteractionFragment
import hu.bme.mit.gamma.scenario.model.LoopCombinedFragment
import hu.bme.mit.gamma.scenario.model.ModalInteraction
import hu.bme.mit.gamma.scenario.model.ModalInteractionSet
import hu.bme.mit.gamma.scenario.model.ModalityType
import hu.bme.mit.gamma.scenario.model.ParallelCombinedFragment
import hu.bme.mit.gamma.scenario.model.Reset
import hu.bme.mit.gamma.scenario.model.ScenarioDeclaration
import hu.bme.mit.gamma.scenario.model.ScenarioDefinition
import hu.bme.mit.gamma.scenario.model.ScenarioModelPackage
import hu.bme.mit.gamma.scenario.model.Signal
import hu.bme.mit.gamma.statechart.composite.SynchronousComponent
import hu.bme.mit.gamma.statechart.interface_.EventDirection
import hu.bme.mit.gamma.statechart.interface_.RealizationMode
import java.util.List
import java.util.Map
import java.util.Set
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors
import org.eclipse.emf.common.util.EList
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.xtend.lib.annotations.Data
import org.eclipse.xtext.EcoreUtil2
import org.eclipse.xtext.validation.Check

import static hu.bme.mit.gamma.scenario.language.util.EcoreUtilWrapper.*

import static extension hu.bme.mit.gamma.scenario.util.CollectionUtil.myGet
import static extension hu.bme.mit.gamma.scenario.util.CollectionUtil.myRemove
import hu.bme.mit.gamma.scenario.model.NegatedModalInteraction
import hu.bme.mit.gamma.scenario.model.StrictAnnotation
import hu.bme.mit.gamma.scenario.model.PermissiveAnnotation
import hu.bme.mit.gamma.scenario.model.NegStrictAnnotation
import hu.bme.mit.gamma.scenario.model.NegPermissiveAnnotation

/**
 * This class contains custom validation rules. 
 * 
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation
 */
class ScenarioLanguageValidator extends AbstractScenarioLanguageValidator {

	extension ExpressionEvaluator expressionEvaluator = ExpressionEvaluator.INSTANCE

	static enum MarkerLevel {
		WARNING,
		ERROR,
		INFO
	}

	@Data
	static class MarkerContext {
		val MarkerLevel level
		val String message
		val EObject target
		val EStructuralFeature feature
		val int index
	}

	val Map<EObject, Set<MarkerContext>> markerMessages

	new() {
		super()
		ValidatorBridge::INSTANCE.validator = this
		markerMessages = new ConcurrentHashMap
	}

	def void removeMarker(MarkerContext ctx) {
		markerMessages.entrySet.findFirst[it.value.contains(ctx)]?.value?.remove(ctx)
	}

	def void addMarker(MarkerContext ctx) {
		synchronized (markerMessages) {
			val markers = markerMessages.myGet(ctx.target)
			if (markers.nullOrEmpty) {
				markerMessages.put(ctx.target, newHashSet(ctx))
			} else {
				markers.add(ctx)
			}
		}
	}

	@Check(NORMAL)
	def void checkIncompatibleAnnotations(ScenarioDefinition s) {
		var strictPresent = false
		var permissivePresent = false
		var negstrictPresent = false
		var negpermissivePresent = false
		for (a : s.annotation) {
			if (a instanceof StrictAnnotation)
				strictPresent = true
			else if (a instanceof PermissiveAnnotation)
				permissivePresent = true
			if (a instanceof NegStrictAnnotation)
				negstrictPresent = true
			else if (a instanceof NegPermissiveAnnotation)
				negpermissivePresent = true
		}
		if (permissivePresent && strictPresent)
			error('''A scenario should be annotated with either a permissive or strict annotation.''',
				ExpressionModelPackage.Literals.NAMED_ELEMENT__NAME)
		if (negpermissivePresent && negstrictPresent)
			error('''A scenario should be annotated with either a permissive or strict annotation with respect to negated sends blocks.''',
				ExpressionModelPackage.Literals.NAMED_ELEMENT__NAME)
	}

	@Check(NORMAL)
	def void clearCustomMarkers(EObject target) {
		markerMessages.myRemove(target)
	}

	@Check
	def void checkCustomMarkers(EObject target) {
		markerMessages.myGet(target)?.forEach[showMarker]
	}

	private def void showMarker(MarkerContext ctx) {
		switch (ctx.level) {
			case INFO: info(ctx.message, ctx.feature, ctx.index)
			case WARNING: warning(ctx.message, ctx.feature, ctx.index)
			case ERROR: error(ctx.message, ctx.feature, ctx.index)
		}
	}

	@Check
	def void checkScenarioNamesAreUnique(ScenarioDeclaration scenarioDeclaration) {
		val duplicateScenarioNames = scenarioDeclaration.scenarios.map[it.name].stream.collect(Collectors::groupingBy([
			it
		], Collectors::counting)).filter[_key, value|value > 1].entrySet.map[it.key].toSet

		if (!duplicateScenarioNames.isEmpty) {
			scenarioDeclaration.scenarios.forEach [
				if (duplicateScenarioNames.contains(it.name)) {
					val index = scenarioDeclaration.scenarios.indexOf(it)
					error('''Scenario names should be unique.''',
						ScenarioModelPackage.Literals.SCENARIO_DECLARATION__SCENARIOS, index)
				}
			]
		}
	}

	@Check
	def void checkAtLeastOneHotSignalInMainchart(ScenarioDefinition scenario) {
		val noHotSignal = everyInteractionIsCold(scenario.chart)
		if (noHotSignal) {
			warning(
				'''There should be at least one hot signal in chart.''',
				ScenarioModelPackage.Literals.SCENARIO_DEFINITION__CHART
			)
		}
	}

	@Check
	def void checkModalInteractionSets(ModalInteractionSet modalInteractionSet) {
		val scenario = EcoreUtil2.getRootContainer(modalInteractionSet) as ScenarioDeclaration
		val component = scenario.component
		val container = modalInteractionSet.eContainer
		if (component instanceof SynchronousComponent) {
			val containedModalInteractionSets = modalInteractionSet.modalInteractions.filter(ModalInteractionSet)
			if (!containedModalInteractionSets.empty) {
				// Just to make sure, in the current grammar this is impossible
				error('''Modal interaction sets cannot contain modal interaction sets.''', container,
					modalInteractionSet.eContainingFeature, modalInteractionSet.index)
			}
		} else {
			error('''Scenarios with respect to asynchronous components cannot contain modal interaction sets.''',
				container, modalInteractionSet.eContainingFeature, modalInteractionSet.index)
		}
	}

	@Check
	def void checkModalInteractionsInSynchronousComponents(ModalInteraction interaction) {
		val scenario = EcoreUtil2.getRootContainer(interaction) as ScenarioDeclaration
		val component = scenario.component
		val container = interaction.eContainer
		if (component instanceof SynchronousComponent) {
			val definition = interaction
			if (definition instanceof ModalInteractionSet || definition instanceof Delay ||
				definition instanceof Reset) {
				// Delays and resets cannot be contained by modal interaction sets
				return
			} // Resets and normal signals must be contained by modal interaction sets 
			else if (!(container instanceof ModalInteractionSet) && !(container instanceof NegatedModalInteraction &&
				container.eContainer instanceof ModalInteractionSet)) {
				error('''Modal interactions in scenarios with respect to synchronous components must be contained by modal interaction sets.''',
					container, interaction.eContainingFeature, interaction.index)
			}
		}
	}

	@Check
	def void checkModalInteractionsInSynchronousComponents(Reset reset) {
		val container = reset.eContainer
		if (container instanceof ModalInteractionSet) {
			val index = container.index
			if (index != 0) {
				val containerContainer = container.eContainer
				error('''Resets must be specified as the first element in a containing set.''', containerContainer,
					container.eContainingFeature, index)
			}
		}
	}

	private def getIndex(EObject object) {
		val container = object.eContainer
		if (container instanceof NegatedModalInteraction)
			return 0;
		val feature = object.eContainingFeature
		val index = (container.eGet(feature) as EList<? extends EObject>).indexOf(object)
		return index
	}

	private def boolean everyInteractionIsCold(Chart chart) {
		chart.fragment.interactions.forall[interactionIsCold]
	}

	private def boolean interactionIsCold(Interaction interaction) {
		switch (interaction) {
			ModalInteractionSet: interaction.modalInteractions.forall[it.interactionIsCold]
			Delay: true
			Reset: true
			ModalInteraction: interaction.modality == ModalityType.COLD
			CombinedFragment: interaction.fragments.forall[interactions.forall[interactionIsCold]]
			default: false
		}
	}

	@Check
	def void checkFirstInteractionsModalityIsTheSame(CombinedFragment combinedFragment) {
		if (combinedFragment.fragments.size > 1) {
			val fragments = combinedFragment.fragments
			val referenceModality = fragments.head.interactions.firstInteractionsModality
			val modalityIsDifferent = !fragments.forall[referenceModality == it.interactions.firstInteractionsModality]
			if (modalityIsDifferent) {
				error('''First interaction's modality should be the same in each fragment belonging to the same combined fragment.''',
					ScenarioModelPackage.Literals.COMBINED_FRAGMENT__FRAGMENTS)
			}
		}
	}

	private def ModalityType getFirstInteractionsModality(List<Interaction> interaction) {
		val first = interaction.head
		switch (first) {
			ModalInteraction: first.modality
			CombinedFragment: first.fragments.head.interactions.firstInteractionsModality
		}
	}

	@Check
	def void checkPortCanSendSignal(Signal signal) {
		if (signal.direction != InteractionDirection.SEND) {
			return
		}

		val errorMessagePrefix = '''Port cannot send'''
		// PROVIDED -> IN is IN, OUT is OUT; REQUIRED -> IN is OUT, OUT is IN  
		val expectedDirections = #{RealizationMode.PROVIDED -> EventDirection.OUT,
			RealizationMode.REQUIRED -> EventDirection.IN}

		checkEventDirections(signal, expectedDirections, errorMessagePrefix)
	}

	@Check
	def void checkPortCanReceiveSignal(Signal signal) {
		if (signal.direction != InteractionDirection.RECEIVE) {
			return
		}

		val errorMessagePrefix = '''Port cannot receive'''
		// PROVIDED -> IN is IN, OUT is OUT; REQUIRED -> IN is OUT, OUT is IN
		val expectedDirections = #{RealizationMode.PROVIDED -> EventDirection.IN,
			RealizationMode.REQUIRED -> EventDirection.OUT}

		checkEventDirections(signal, expectedDirections, errorMessagePrefix)
	}

	@Check(NORMAL)
	def void negatedReceives(NegatedModalInteraction nmi) {
		var mi = nmi.modalinteraction
		if (mi instanceof Signal) {
			if (mi.direction == InteractionDirection.RECEIVE)
				info('''Currently negated interactions received by the component are not processed''',
					ScenarioModelPackage.Literals.NEGATED_MODAL_INTERACTION__MODALINTERACTION)
		}
	}

	@Check
	def void checkParallelCombinedFragmentExists(ParallelCombinedFragment fragment) {
		val parent = (fragment.eContainer as InteractionFragment)
		val index = parent.interactions.indexOf(fragment)
		info('''Beware that a parallel combined fragment will introduce every possible partial orderings of its fragments. It may have a significant impact on the performance.''',
			parent, ScenarioModelPackage.Literals.INTERACTION_FRAGMENT__INTERACTIONS, index)
	}

	private def void checkEventDirections(Signal signal, Map<RealizationMode, EventDirection> directionByMode,
		String errorMessagePrefix) {
		val portRealizationMode = signal.port.interfaceRealization.realizationMode

		val signalInterf = signal.port.interfaceRealization.interface
		val signalEvent = signal.event

		val interfaceEventDeclarations = StatechartLanguageUtil::collectInterfaceEventDeclarations(signalInterf)
		val signalEventDeclarations = interfaceEventDeclarations.filter[it.event == signalEvent]

		val expectedDirection = directionByMode.get(portRealizationMode)
		val expectedDirections = #[expectedDirection, EventDirection.INOUT]

		val eventDirectionIsWrong = signalEventDeclarations.findFirst[expectedDirections.contains(it.direction)]
		if (isNull(eventDirectionIsWrong)) {
			error('''«errorMessagePrefix» this event, because of incompatible port mode. Should the port be «RealizationMode.PROVIDED», set the event to be «directionByMode.get(RealizationMode.PROVIDED)»; should the port be «RealizationMode.REQUIRED», set the event to be «directionByMode.get(RealizationMode.REQUIRED)».''',
				ScenarioModelPackage.Literals.SIGNAL__EVENT)
		}
	}

	@Check
	def void checkIntervals(LoopCombinedFragment loop) {
		checkIntervals(loop.minimum, loop.maximum, ScenarioModelPackage.Literals.LOOP_COMBINED_FRAGMENT__MINIMUM)
	}

	@Check
	def void checkIntervals(Delay delay) {
		checkIntervals(delay.minimum, delay.maximum, ScenarioModelPackage.Literals.DELAY__MINIMUM)
	}

	private def checkIntervals(Expression minimum, Expression maximum, EStructuralFeature feature) {
		try {
			val min = minimum.evaluateInteger
			if (min < 0) {
				error('''The minimum value must be greater than or equals to 0.''', feature)
			}
			if (maximum !== null) {
				val max = maximum.evaluateInteger
				if (min > max) {
					error('''The minimum value must not be greater than the maximum value.''', feature)
				}
			}
		} catch (IllegalArgumentException e) {
			error('''Both the minimum and maximum values must be of type integer.''', feature)
		}
	}

//	@Check
//	def void checkArgumentTypes(Signal signal) {
//		val arguments = signal.arguments
//		val parameterDeclarations = signal.event.parameterDeclarations
//		if (arguments.size != parameterDeclarations.size) {
//			error("The number of arguments must match the number of parameters.",
//				ExpressionModelPackage.Literals.ARGUMENTED_ELEMENT__ARGUMENTS)
//			return
//		}
//		if (!arguments.empty && !parameterDeclarations.empty) {
//			for (var i = 0; i < arguments.size && i < parameterDeclarations.size; i++) {
//				checkTypeAndExpressionConformance(parameterDeclarations.get(i).type, arguments.get(i),
//					ExpressionModelPackage.Literals.ARGUMENTED_ELEMENT__ARGUMENTS)
//			}
//		}
//	}

	@Check
	def void checkUnsupportedElementsForCompatibility(ScenarioDefinition scenario) {
		if (EcoreUtil2.getAllContentsOfType(scenario, Signal).exists[!it.arguments.empty]) {
			info("Compatibility decision is not supported if the scenario contains argumented signals.",
				ExpressionModelPackage.Literals.NAMED_ELEMENT__NAME)
		}
		if (!EcoreUtil2.getAllContentsOfType(scenario, Delay).empty) {
			info("Compatibility decision is not supported if the scenario contains delays.",
				ExpressionModelPackage.Literals.NAMED_ELEMENT__NAME)
		}
	}

}
