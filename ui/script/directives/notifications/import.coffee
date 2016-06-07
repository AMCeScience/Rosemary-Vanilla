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

module.directive 'notificationimport', ->
  restrict: 'E'
  scope:
    data: '='
  template: """
    <a ng-click="ctrl.go()">
      <i class="menu-icon fa fa-cloud-download bg-light-blue"></i>
      <div class="menu-info">
        <h4 class="control-sidebar-subheading">Import complete</h4>
        <p>{{ctrl.counts | plural | toSentence | capitalize}} have been imported</p>
        <p>{{data.info.created | fromnow}}</p>
      </div>
    </a>
  """
  # <div class="desc">{{ctrl.counts | plural | toSentence | capitalize}} have been imported</div>
  #       <span class="separator">•</span>
  #       <span class="name">{{ctrl.author.name}}</span>
  controller: ($scope, $rootScope, $q, Object, Tools, User, Tags, Nav) ->

    new class ImportNotification extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

        @counts = _ @scope.data.info.dict
        .pick (value, key) -> _.startsWith key, "count"
        .transform (result, value, key) -> result[key.substring 5] = parseInt value.value
        .pick (value, key) -> value > 0
        .value()

        User.users.all().then =>
          @author = User.users.syncGet(@scope.data.actor)

      ###########################
      # Methods                 #
      ###########################

      go: => Nav.data @scope.data.imported