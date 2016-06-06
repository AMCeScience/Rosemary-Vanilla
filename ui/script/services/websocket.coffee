module = angular.module 'Rosemary'
module.factory 'Websocket', ($q, $rootScope, Auth) ->

  new class Websocket

    ###########################
    # Instance variables      #
    ###########################

    ###########################
    # Constructor & init      #
    ###########################

    constructor: ->
      @root = $rootScope
      @connect()

      $rootScope.$on 'user_login', -> @connect()

    ###########################
    # Methods                 #
    ###########################

    connect: =>
      @socket = new WebSocket 'ws://' + location.host + '/socket'
      @socket.onmessage = @in
      @socket.onopen = @open

    in: (event) =>
      data = JSON.parse event.data
      @root.$emit "websocket:" + data.kind, data.data

    open: =>
      @socket.send JSON.stringify
        kind: "auth"
        data: Auth.getToken()