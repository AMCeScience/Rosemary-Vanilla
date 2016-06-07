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

module.directive 'notificationuserprocessing', ->
  restrict: 'E'
  scope:
    data: '='
  template: """
    <a ng-click="ctrl.go()">
      <i class="menu-icon fa fa-cog bg-green"></i>
      <div class="menu-info">
        <h4 class="control-sidebar-subheading">{{data.actor.name}}</span> has {{data.action}} processing <span class="title">{{data.processing.name}}</h4>
        <p class="date">{{data.info.created | fromnow}}</p>
      </div>
    </a>
  """
  controller: ($scope, $rootScope, $q, $state, Object, Remote, User) ->

    new class UserProcessingNotification extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

        Remote.one 'processing-groups', @scope.data.processing # NEW_API
        .then (processing) =>
          @scope.data.processing = processing

        User.users.get @scope.data.actor
        .then (user) => @scope.data.actor = user
          
      ###########################
      # Methods                 #
      ###########################