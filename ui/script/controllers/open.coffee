module = angular.module 'Rosemary'
module.controller 'OpenController', ($scope, $rootScope) ->

  new class OpenCtrl extends DefaultController

    ###########################
    # Instance variables      #
    ###########################

    ###########################
    # constructor & init      #
    ###########################

    # Always call super with scope
    constructor: ->
      super $scope, $rootScope

    ###########################
    # Methods                 #
    ###########################