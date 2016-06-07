module = angular.module 'Rosemary'
module.controller 'LoginController', ($scope, $rootScope, $state, Auth, Session) ->

  new class LoginCtrl extends DefaultController

    ###########################
    # Instance variables      #
    ###########################

    credentials:
     email: "admin@rosemary.ebioscience.amc.nl"
     password: "secret"

    ###########################
    # constructor & init      #
    ###########################

    # Always call super with scope
    constructor: -> super $scope, $rootScope

    # Gets called automaticlly
    init: ->
      msg = Session.get "logout_msg"
      if not _.isUndefined msg
        @root.$emit 'alert', new Alert("danger", "<b>Logout reason:</b> #{msg}")

      $('input').iCheck
        checkboxClass: 'icheckbox_square-blue'

    ###########################
    # Methods                 #
    ###########################

    login: =>
      Auth.login @credentials
      .then (-> $rootScope.nav.data()), (-> alert "Not OK!")