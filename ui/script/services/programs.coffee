module = angular.module 'Rosemary'
module.factory 'Programs', (Remote) ->

  new class Programs

    ###########################
    # Instance variables      #
    ###########################

    ###########################
    # Constructor & init      #
    ###########################

    constructor: ->
      @all = Remote.all 'recipes/applications'

    ###########################
    # Methods                 #
    ###########################