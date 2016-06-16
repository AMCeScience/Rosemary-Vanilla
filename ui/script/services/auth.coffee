# Copyright (C) 2016  Academic Medical Center of the University of Amsterdam (AMC)
#  
# This program is semi-free software: you can redistribute it and/or modify it
# under the terms of the Rosemary license. You may obtain a copy of this
# license at:
# 
# https://github.com/AMCeScience/Rosemary-Vanilla/blob/master/LICENSE.md
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the License for the specific language governing permissions and
# limitations under the License.
#  
# You should have received a copy of the Rosemary license
# along with this program. If not, 
# see https://github.com/AMCeScience/Rosemary-Vanilla/blob/master/LICENSE.md.
#  
#        Project: https://github.com/AMCeScience/Rosemary-Vanilla
#        AMC eScience Website: http://www.ebioscience.amc.nl/

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
        # Set a watch on the token cookie
        # Once it has been updated emit the user_login message
        $rootScope.$watch (-> $cookies["XSRF-TOKEN"]), =>
          if $cookies["XSRF-TOKEN"]
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