module = angular.module 'Rosemary'
module.controller 'UsersController', ($scope, $rootScope, $state, $stateParams, $q, Remote, Object) ->

  new class UsersCtrl extends DefaultController

    ###########################
    # Instance variables      #
    ###########################

    @users = []

    ###########################
    # constructor & init      #
    ###########################

    # Always call super with scope
    constructor: ->
      super $scope, $rootScope

    init: =>
      @root.breadcrumb = [
        'Users'
      ]

      @load()
        

    ###########################
    # Methods                 #
    ###########################

    load: =>
      Remote.get 'users'
      .then (@users) =>
