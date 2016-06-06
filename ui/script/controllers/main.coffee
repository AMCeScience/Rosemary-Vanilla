module = angular.module 'Rosemary'
module.controller 'MainController', ($scope, $rootScope, $state, Auth, Nav, Tags, Local, Session, Object, Workspace, Websocket) ->

  ctrl = new class MainCtrl extends DefaultController

    ###########################
    # Instance variables      #
    ###########################

    workspaces: []
    new_workspace: ""
    create_workspace_mode: false
    search_options: SEARCH_OPTIONS

    ###########################
    # constructor & init      #
    ###########################

    # Always call super with scope
    constructor: ->
      super $scope, $rootScope

    init: =>
      @root.loading = true

      # Default localstorage sync with root scope
      Local.sync 'basket', []
      Object.uniqueAll @root.basket

      @name = Auth.getUser().name
      $rootScope.$on 'scope_change', ->
        $rootScope.ctrl.clearBasket()

      # Call admin functions to fix layout once page is loaded
      $.AdminLTE.layout.fix()
      $.AdminLTE.layout.fixSidebar()

    ###########################
    # Methods                 #
    ###########################

    logout: ->
      Auth.logout()