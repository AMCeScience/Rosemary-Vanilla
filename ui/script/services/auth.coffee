module = angular.module 'Rosemary'
module.factory 'Auth', ($injector, $q, $window, $cookies, $rootScope, Remote, Session, Local, Object) ->

  new class Auth

    user_key: 'user'
    user_cache: null

    ###########################
    # Methods                 #
    ###########################

    login: (credentials) ->
      $rootScope.loading = true

      success = (user) => 
        @setUser user
        $rootScope.$emit 'user_login'
      error   = ->
        $rootScope.loading = false
        $q.reject "Login failed"
        
      Session.clear()
      Local.clear()

      Remote
      .post "login", credentials
      .then success, error

    register: (credentials) =>
      success =        => $injector.get('$state').go 'open.login'
      error   = (data) -> $q.reject data

      Remote
      .put "users", credentials
      .then success, error

    logout: (msg) ->
      $injector.get('$state').go 'open.login'
      
      if not _.isUndefined msg
        Session.set "logout_msg", msg
      
      Remote.get "logout" # NEW_API
      .finally -> $window.location.reload()
  
    isLoggedIn: =>
      Local.has @user_key
  
    getUser: =>
      if _.isNull @user_cache
        @user_cache = Local.get @user_key
      @user_cache

    getUserId: =>
      Object.toId @getUser()
  
    setUser: (user) =>
      @user_cache = user
      Local.set @user_key, user

    isUser: (obj) =>
      Object.equal obj, @getUser()

    getToken: -> $cookies['XSRF-TOKEN']