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

module = angular.module 'Rosemary'
module.controller 'CommunicationController', ($scope, $rootScope, $q, $state, User, Remote, Object, Tags, Auth, Nav) ->

  new class CommunicationCtrl extends DefaultController

    ###########################
    # Instance variables      #
    ###########################

    ###########################
    # constructor & init      #
    ###########################

    threads: []
    selected: null
    reply: ""

    # Always call super with scope
    constructor: ->
      super $scope, $rootScope

    init: =>
      @root.breadcrumb = [ 'Communication' ]
      @workspace_watch = @root.$watch "workspace", @update

    destroy: =>
      @workspace_watch()

    update: =>
      $q.all [ User.users.all(), Tags.workspace_tags ]
      .then (users, tags) =>
        Remote.post 'threads', # NEW_API
          workspace: Object.toId @root.workspace
        .then (threads) =>
          if threads.length is 0
            $state.go 'main.new_message'
          else 
            @threads = _.map threads, (thread) ->
              thread.last = _.first thread.messages
              thread.messages = _.forEach thread.messages, (message) ->
                message.rights.owner = User.users.syncGet message.rights.owner
                message.info.dict.countData = parseInt message.info.dict.countData.value
                message.info.dict.countProcessing = parseInt message.info.dict.countProcessing.value
                message.info.dict.countProcessingGroup = parseInt message.info.dict.countProcessingGroup.value
                message.info.dict.count = message.info.dict.countData + message.info.dict.countProcessing + message.info.dict.countProcessingGroup
              thread

            @selected =
              if @root.tmp.thread then _.find @threads, (threads) =>
                Object.equal threads.thread, @root.tmp.thread
              else _.first @threads
              
    ###########################
    # Methods                 #
    ###########################

    select: (@selected) =>

    send: =>
      Remote.put 'threads', # NEW_API
        name: @selected.last.name
        body: @reply
        owner: Auth.getUserId()
        thread: Object.toId @selected.thread
      .then ->
        $state.go 'main.communication', {}, { reload: true }

    focusReply: ->
      $(".reply")[0].focus()

    moreOptions: ->
      @root.message.name = @selected.last.name
      @root.message.body = @reply
      @root.message.thread = Object.toId @selected.thread
      $state.go 'main.new_message'

    showData: (message) ->
      Nav.data message

    showProcessing: (message) ->
      Nav.processings message

    showProcessingGroup: (message) ->
      Nav.processingGroups message