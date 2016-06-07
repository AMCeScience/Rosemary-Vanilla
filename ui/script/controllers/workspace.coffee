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
module.controller 'WorkspaceController', ($scope, $rootScope, $state, Object, Workspace, User, Tools, Auth) ->

  new class WorkspaceCtrl extends DefaultController

    ###########################
    # Instance variables      #
    ###########################

    members: []
    user_search: ""
    user_search_suggestions: []
    delete_check: ""

    ###########################
    # constructor & init      #
    ###########################

    # Always call super with scope
    constructor: ->
      super $scope, $rootScope

    init: =>
      $rootScope.$watch 'workspace', (workspace) =>
        if _.isObject workspace then @initWithWorkspace()

    initWithWorkspace: =>
      @root.breadcrumb = [
        'Workspace'
        @root.workspace.name
        'Settings'
      ]

      @users = User.users.all()

      Workspace.getMembers(@root.workspace).then (@members) =>
        @initBloodhound()

    initBloodhound: =>
      @users.then (users) =>
        @users_search = _.filter users, (user) =>
          valid = not Object.equal user, @root.workspace.rights.owner
          valid = valid and not _.any @members, Object.equalFn user
          valid

        @bloodhound = Tools.bloodhound @users_search
        @scope.$watch "ctrl.user_search", =>
          @bloodhound.get @user_search, (@user_search_suggestions) =>

    ###########################
    # Methods                 #
    ###########################

    addMember: (user) =>
      Workspace.addMember @root.workspace, user
      .then @updateWorkspace

    removeMember: (user) =>
      Workspace.removeMember @root.workspace, user
      .then @updateWorkspace

    updateWorkspace: (workspace) =>
      @root.workspace = Object.unique workspace
      @initWithWorkspace()

    delete: =>
      Workspace.deleteWorkspace @root.workspace
      .then =>
        @root.workspace = null
        $rootScope.nav.data()