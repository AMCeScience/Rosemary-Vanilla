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

module.filter 'to_id', (Object)   -> (data)           -> Object.toId data
module.filter 'to_type', (Object) -> (data)           -> Object.toType data
module.filter 'replace',          -> (string, param)  -> _.template(string)(param)
module.filter 'moment',           -> (string)         -> moment string
module.filter 'fromnow',          -> (m)              -> moment(m).fromNow()
module.filter 'prune',            -> (string, length) -> _.prune string, length
module.filter 'capitalize',       -> (string)         -> _.capitalize string
module.filter 'toSentence',       -> (string)         -> _.toSentence string
module.filter 'last',             -> (arr)            -> _.last arr
module.filter 'first',             -> (arr)           -> _.first arr

module.filter 'plural', ->
  (counts) -> 
    Tools = injector.get "Tools"
    _.map counts, (value, key) -> Tools.plural key, value

module.filter 'status_to_color', ->
  (status) ->
    switch status
      when 'Submitted'      then 'info'     
      when 'On Hold'        then 'warning'  
      when 'Suspended'      then 'warning'       
      when 'In Preparation' then 'active'
      when 'In Progress'    then 'active'   
      when 'Done'           then 'success'
      when 'Failed'         then 'danger'        
      when 'Aborted'        then 'danger'       