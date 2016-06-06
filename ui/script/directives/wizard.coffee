#FIX: It is possible to swap between steps and click 'yes import' while no projects have been selected.

module = angular.module 'Rosemary'

module.directive 'wizard', ->
  restrict: 'E'
  transclude: true
  scope: true
  template: """
  <div class="wizard">
    <center>
      <button ng-repeat="step in ctrl.steps" class="btn wizard" ng-class="{'btn-primary': ctrl.isActive(step)}" ng-click="ctrl.activate(step)">
        <span class="badge" class="badge-info">Step {{$index + 1}}</span> {{step.getName()}}
      </button>
    </center>
  </div>
  <div class="wizard-content" ng-transclude></div>
  """
  link: (scope, element, attrs) ->
    scope.$parent.ctrl.next = scope.ctrl.next
  controller: ($scope) ->

    @instance = new class Wizard extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      steps: []
      active: null

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope

      ###########################
      # Methods                 #
      ###########################

      addStep: (step) =>
        @activate step if @steps.length is 0
        @steps.push step
        @steps = _.sortBy @steps, (step) -> step.scope.order

      removeStep: (step) => _.remove @steps, step

      activate: (step) =>
        @active?.setActive false
        @active = step
        @active.setActive true

      next: =>
        current = _.indexOf @steps, @active
        @activate @steps[current + 1]

      isActive: (step) =>
        stepIndex = @steps.findIndex (element) -> element is step
        activeIndex = @steps.findIndex (element) => element is @active
        stepIndex <= activeIndex

module.directive 'step', ->
  restrict: 'E'
  require: "^wizard"
  transclude: true
  scope:
    name: '@'
    order: '@'
  template: '<div class="step" ng-transclude ng-show="ctrl.active"></div>'
  link: (scope, element, attrs, wizard) ->
    wizard.instance.addStep scope.ctrl
    scope.$on "$destroy", -> wizard.instance.removeStep scope.ctrl
  controller: ($scope) ->

    new class Step extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      active: false

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope

      ###########################
      # Methods                 #
      ###########################

      getName: -> $scope.name

      setActive: (@active) ->