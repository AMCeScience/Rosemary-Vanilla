class DefaultController

  constructor: (@scope, @root) ->
    @scope.root = @root
    @scope.ctrl = @

    @init()

    @scope.$on '$destroy', @destroy

  init: ->

  destroy: ->