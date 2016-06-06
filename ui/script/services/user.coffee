module = angular.module 'Rosemary'
module.factory 'User', (Remote, Object) ->

  new class User

    ###########################
    # Instance variables      #
    ###########################

    ###########################
    # Constructor & init      #
    ###########################

    constructor: ->
      @users = new RemoteCollection -> Remote.all 'users' # NEW_API

    ###########################
    # Methods                 #
    ###########################