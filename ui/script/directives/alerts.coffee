class Alert

  constructor: (@type, @content)->

module = angular.module 'Rosemary'

module.directive 'rmalerts', ->
  restrict: 'E'
  scope: true
  template: """
    <div>
      <rmalert ng-repeat="alert in ctrl.alerts" data="alert"></rmalert>
    </div>
  """
  controller: ($scope, $rootScope, Object) ->

    @instance = new class RMAlerts extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      alerts: []

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

        @root.$on "alert", (info, alert) =>
          @alerts.push alert

        @root.$on "$stateChangeStart", =>
          @alerts = []

      ###########################
      # Methods                 #
      ###########################

      remove: (alert) =>
        _.remove @alerts, alert

module.directive 'rmalert', ->
  restrict: 'E'
  require: "^rmalerts"
  scope: 
    data: '='
  link: (scope, element, attrs, rmalerts) ->
    scope.ctrl.alerts = rmalerts.instance
  template: """
    <div class="alert alert-{{data.type}}">
      <button type="button" class="close" ng-click="ctrl.click()">
        <span>&times;</span>
      </button>
      <i class="fa fa-{{data.type}}-circle"></i>
      <span ng-bind-html="data.content"></span>
    </div>
  """
  controller: ($scope, $rootScope, Object) ->

    new class RMAlert extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      type: 'warning' # success, info, warning, danger

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

      ###########################
      # Methods                 #
      ###########################

      click: =>
        @alerts.remove @scope.data
