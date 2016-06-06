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