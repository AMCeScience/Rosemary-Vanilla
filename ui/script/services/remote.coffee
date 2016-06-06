###
Basic wrapper around remote collection. Initialize with callback so it can do the request when necessary.
Also has sync version for methods to overcome some standard sync/async difficulties
###
class RemoteCollection

  data: null
  data_sync: []

  constructor: (@remote) ->

  delay: (method) =>
    if _.isNull @data
      @data = @remote()
      @data.then (@data_sync) =>
    @data.then method

  all: =>
    @delay @syncAll

  get: (id) =>
    @delay => @syncGet id

  syncAll: =>
    @data_sync

  syncGet: (id) =>
    Object = injector.get 'Object'
    _.find @data_sync, Object.equalFn id

###
This class/service is the main method to communicate with the backend. It takes care of the 
unique problem (see object) and dependency loop problems.
###
module = angular.module 'Rosemary'
module.factory 'Remote', ($injector, Object) ->

  new class

    ###########################
    # Instance variables      #
    ###########################

    API_URL: '/api/v1/'
    CONN: null

    ###########################
    # Constructor & init      #
    ###########################

    ###########################
    # Methods                 #
    ###########################

    _http: =>
      if @CONN is null
        @CONN = $injector.get "$http"
        # @CONN.defaults.cache = true
      @CONN

    # GET single with unique
    one: (type, id) =>
      @get type + '/' + Object.toId id
      .then (r) -> Object.unique r

    # GET array with unique
    all: (type) =>
      @get type
      .then (r) -> Object.uniqueAll r

    # POST with unique
    query: (url, data) =>
      @post url, data
      .then (r) ->
        Object.uniqueAll r

    put: (method, data) =>
      @_http()
      .put @API_URL + method, data
      .then (r) -> r.data

    # DELETE post enabled with JSON data
    delete: (method, data = {}) =>
      @_http()
      .delete @API_URL + method,
        data: data
      .then (r) -> r.data

    get: (method) =>
      @_http()
      .get @API_URL + method
      .then (r) -> r.data

    post: (method, data) =>
      @_http()
      .post @API_URL + method, data
      .then (r) -> r.data