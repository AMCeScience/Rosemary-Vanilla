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

class LiveImport

  counts: {}

  constructor: (data) ->
    @id = data.id
    @update data

  update: (data) =>
    @state = data.state
    @updated = moment()

    if @state is "running"
      @counts[data.type] =
        if _.has @counts, data.type then @counts[data.type] + 1
        else 1

module.directive 'livenotificationimport', ->
  restrict: 'E'
  scope:
    data: '='
  template: """
    <a ng-if="data.state != 'complete'">
      <i class="menu-icon fa fa-cloud-download bg-light-blue"></i>
      <div class="menu-info">
        <h4 class="control-sidebar-subheading">Import progressing...</h4>
        <p ng-if="data.state == 'start'">A new import has been started</p>
        <p ng-if="data.state == 'running'">{{data.counts | plural | toSentence | capitalize}} have been imported</p>
        <p>{{data.updated | fromnow}}</p>
      </div>
    </a>
  """