module = angular.module 'Rosemary'
module.factory 'Root', ($rootScope, Nav, Import, Object) ->

  new class RootCtrl

    constructor: ->
      @scope = $rootScope
      @scope.ctrl = @
      @scope.tmp = {}
      @scope.nav = Nav
      @scope.search =
        type: SEARCH_DEFAULT,
        query: ""
        tags: []
      @scope.message =
        to: []
      @scope.breadcrumb = []

    clearBasket: =>
      @scope.basket = []

    setBasket: (data) =>
      @scope.basket = data.slice 0