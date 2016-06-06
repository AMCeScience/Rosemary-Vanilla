module = angular.module 'Rosemary'
module.factory 'Import', ($rootScope, $state) ->

  new class Import

    ###########################
    # Instance variables      #
    ###########################

    imports: {}

    ###########################
    # Constructor & init      #
    ###########################

    constructor: ->
      @root = $rootScope
      @root.imports = @imports

      @root.$on "websocket:import", (event, data) => @processUpdate data

    ###########################
    # Methods                 #
    ###########################

    processUpdate: (data) =>
      if not _.has @imports, data.id
        @imports[data.id] = 
          data: {}
      current = @imports[data.id]
      current.state = data.state
      if data.state is "running"
        if _.has current.data, data.type
          current.data[data.type] = current.data[data.type] + 1
        else 
          current.data[data.type] = 1

      @root.$apply()
      @root.$emit "imports"

      if data.state is "complete"
        $rootScope.nav.data()