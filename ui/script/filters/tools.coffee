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