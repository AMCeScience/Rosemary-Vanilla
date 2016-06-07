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