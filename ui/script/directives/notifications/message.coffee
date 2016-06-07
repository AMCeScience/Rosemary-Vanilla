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

module.directive 'notificationmessage', ->
  restrict: 'E'
  scope:
    data: '='
  template: """
    <a ng-click="ctrl.go()">
      <i class="menu-icon fa fa-envelope bg-red"></i>
      <div class="menu-info">
        <h4 class="control-sidebar-subheading">{{ctrl.author.name}} send a message in <span class="title">{{data.info.dict.subject.value}}</span></h4>
        <p ng-if="ctrl.receivers.length > 0"> to {{ctrl.receivers | toSentence}}</p>
        <p class="date">{{data.info.created | fromnow}}</p>
      </div>
    </a>
  """
  # <p ng-if="data.info.dict.count > 0">
  #   <span class="separator">•</span>
  #   <span ng-if="data.info.dict.countData > 0">
  #     <span class="fa fa-paperclip"></span>
  #     {{data.info.dict.countData}}
  #   </span>
  #   <span ng-if="data.info.dict.countProcessing > 0">
  #     <span class="fa fa-cog"></span>
  #     {{data.info.dict.countProcessing}}
  #   </span>
  # </p>
  controller: ($scope, $rootScope, $q, $state, Object, Tools, User, Tags, Nav) ->

    new class MessageNotification extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

        @scope.data.info.dict.countData = parseInt @scope.data.info.dict.countData
        @scope.data.info.dict.countProcessing = parseInt @scope.data.info.dict.countProcessing
        @scope.data.info.dict.count = @scope.data.info.dict.countData + @scope.data.info.dict.countProcessing

        $q.all [ User.users.all(), Tags.tags ]
        .then =>
          @author = User.users.syncGet(@scope.data.actor)
          @workspace = Tags.syncGet @scope.data.workspace
          @receivers =
            _ @scope.data.receivers
            .map User.users.syncGet
            .map (user) -> user.name
            .value()

      ###########################
      # Methods                 #
      ###########################

      go: => 
        @root.tmp.thread = @scope.data.thread
        $state.go 'main.communication', {}, { reload: true }