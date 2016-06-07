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

module.directive 'notificationworkspace', ->
  restrict: 'E'
  scope:
    data: '='
  template: """
    <a ng-click="ctrl.go()">
      <i class="menu-icon fa fa-user bg-yellow"></i>
      <div class="menu-info">
        <h4 class="control-sidebar-subheading">{{ctrl.affected.name}}</span> was {{data.action}} by <span class="title">{{ctrl.author.name}}</span> to the workspace</h4>
        <p class="date">{{data.info.created | fromnow}}</p>
      </div>
    </a>
  """
  controller: ($scope, $rootScope, $q, $state, Object, Tools, User, Tags, Nav) ->

    new class WorkspaceNotification extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

        User.users.all().then =>
          @author = User.users.syncGet @scope.data.actor
          @affected = User.users.syncGet @scope.data.affected

      ###########################
      # Methods                 #
      ###########################

      go: -> $state.go 'main.workspace'