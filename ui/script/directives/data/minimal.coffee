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

module.directive 'dataminimal', () ->
  restrict: 'E'
  templateUrl: 'views/directives/data/minimal.html'
  scope:
    datum: '='
  controller: ($scope, $rootScope, Data) ->

    new class MinimalData extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

      ###########################
      # Methods                 #
      ###########################