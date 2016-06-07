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

module.directive 'dataprocessing', (RecursionHelper) ->
  restrict: 'E'
  templateUrl: 'views/directives/data/processing.html'
  scope:
    selectable: '='
    processing: '='
    color: '@'
  compile: (element, attr) ->
    attr.color = "white" if not attr.color?
    RecursionHelper.compile element
  controller: ($scope, $rootScope, Object, Remote) ->

    new class ProcessingData extends DefaultController

      ###########################
      # Instance variables      #
      ###########################

      processing_data: false

      ###########################
      # Constructor & init      #
      ###########################

      constructor: ->
        super $scope, $rootScope

        @style = { background: @scope.color }

      getData: =>
        if @processing_data then return
        else @processing_data = true

        ids = 
          _.union @scope.processing.inputs, @scope.processing.outputs
          .filter (d) -> _.has d, 'datum'
          .map (d) -> Object.toId d.datum.datum
        
        Remote.query Object.toId(@root.workspace) + '/data/query/ids', { ids: ids }
        .then (data) =>
          if data.length > 0
            # Build Index
            index = {}
            _.forEach data, (d) -> index[Object.toId d] = d

            _.forEach @scope.processing.inputs, (d) ->
              d.datum.datum = index[Object.toId d.datum.datum] 

            _.forEach @scope.processing.outputs, (d) ->
              d.datum.datum = index[Object.toId d.datum.datum]

      childColor: =>
        tinycolor(@scope.color).darken(3).toString()