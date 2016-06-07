# Copyright (C) 2016  Academic Medical Center of the University of Amsterdam (AMC)
#  
# This program is semi-free software: you can redistribute it and/or modify it
# under the terms of the Rosemary license. You may obtain a copy of this
# license at:
# 
# https://github.com/AMCeScience/Rosemary-Vanilla/blob/master/LICENSE.md
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the License for the specific language governing permissions and
# limitations under the License.
#  
# You should have received a copy of the Rosemary license
# along with this program. If not, 
# see https://github.com/AMCeScience/Rosemary-Vanilla/blob/master/LICENSE.md.
#  
#        Project: https://github.com/AMCeScience/Rosemary-Vanilla
#        AMC eScience Website: http://www.ebioscience.amc.nl/

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
