class Storage

  constructor: (@storage, @root) ->

  ###########################
  # Methods                 #
  ###########################

  set: (k, v) =>
    if _.isNull(v) or _.isUndefined(v) then @remove k
    else @storage.setItem k, JSON.stringify v

  get: (k, def = undefined) =>
    v = @storage.getItem k
    if not _.isNull v then JSON.parse v
    else def

  has: (k) =>
    null isnt @storage.getItem k

  remove: (k) =>
    @storage.removeItem k

  clear: =>
    @storage.clear()

  sync: (key, def = undefined, scope = @root) =>
    scope[key] = @get key, def
    scope.$watch key, (val) =>
      @set key, val

module = angular.module 'Rosemary'

module.factory 'Session', ($rootScope) ->
  new class extends Storage
    constructor: -> super sessionStorage, $rootScope

module.factory 'Local', ($rootScope) ->
  new class extends Storage
    constructor: -> super localStorage, $rootScope