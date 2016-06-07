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
module.controller 'NewMessageController', ($scope, $rootScope, $q, $state, User, Remote, Object, Tags, Auth, Tools) ->

  new class NewMessageCtrl extends DefaultController

    ###########################
    # Instance variables      #
    ###########################

    user_search: ""
    user_search_suggestions: []

    ###########################
    # constructor & init      #
    ###########################

    # Always call super with scope
    constructor: ->
      super $scope, $rootScope

      @root.breadcrumb = [ 'Communication', 'New message' ]

      @users = User.users.all()
      @initBloodhound()

    initBloodhound: =>
      @users.then (users) =>
        @bloodhound = Tools.bloodhound users
        @scope.$watch "ctrl.user_search", =>
          @bloodhound.get @user_search, (@user_search_suggestions) =>

    ###########################
    # Methods                 #
    ###########################

    send: =>
      data = if @root.message.thread
        name: @root.message.name
        body: @root.message.body
        owner: Auth.getUserId()
        thread: @root.message.thread
      else
        name: @root.message.name
        body: @root.message.body
        owner: Auth.getUserId()
        workspace: Object.toId @root.workspace
        receivers: _.map @root.message.to, Object.toId

      if @root.message.attached
        dataids = _ @root.basket
        .filter (datum) -> Object.toType(datum) == 'datum'
        .map Object.toId
        .value()

        processingids = _ @root.basket
        .filter (processing) -> Object.toType(processing) == 'processing'
        .map (processing) -> Object.toId processing
        .value()

        processinggroupids = _ @root.basket
        .filter (processing_group) -> Object.toType(processing_group) == 'processing_group'
        .map (processing_group) -> Object.toId processing_group
        .value()
        
        data.dataids = dataids
        data.processingids = processingids
        data.processinggroupids = processinggroupids

      Remote.put 'threads', data # NEW_API
      .then =>
        @reset()
        Tags.init()
        .then -> $state.go 'main.communication'

    reset: =>
      @root.message =
        to: []      

    addUser: (user) =>
      if not _.contains @root.message.to, user
        @root.message.to.push user
      @user_search = ""

    removeUser: (user) =>
      _.remove @root.message.to, user

    editBasket: =>
      @root.tmp.message_data = true
      $rootScope.nav.data()