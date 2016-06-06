###

Central place for generic object/model processing returned from the backend.

Problem origins as follows:
- Data can be returned using different API calls on the same page
- This results in multiple client-side instances of same object
- When modifying the one of the instances, the other doesn't reflect new changes.

This class tries to fix that. It tries to ensure that objects have only one instance, 
and that that instance is up to date. The methods Remote.one, Remote.all and Remote.query
automatically their results using this class.

This class has a few important methods:
- toId: This method returns you the ID of anything containing an ID. This can be an 
  instance, reference or ID itself. The unique method uses this to ID the objects.
- unique: This method returns the unique versions of the instance

###

TAG_WORKSPACE_TYPE = "nl.amc.ebioscience.rosemary.models.WorkspaceTag"
TAG_CATEGORY_TYPE = "nl.amc.ebioscience.rosemary.models.CategoryTag"
TAG_USER_TYPE = "nl.amc.ebioscience.rosemary.models.UserTag"
TAG_PROCESSING_CATEGORY_TYPE = "nl.amc.ebioscience.rosemary.models.ProcessingCategoryTag"
TAG_PROCESSING_STATUS_TYPE = "nl.amc.ebioscience.rosemary.models.ProcessingStatusTag"
TAG_DATUM_CATEGORY_TYPE = "nl.amc.ebioscience.rosemary.models.DatumCategoryTag"
TAG_MESSAGE_TYPE = "nl.amc.ebioscience.rosemary.models.MessageTag"

PROCESSING_GROUP_TYPE = 'nl.amc.ebioscience.rosemary.models.ProcessingGroup'
PROCESSING_TYPE = 'nl.amc.ebioscience.rosemary.models.Processing'
DATUM_TYPE = 'nl.amc.ebioscience.rosemary.models.Datum'

SEARCH_OPTIONS = [
  {id:"", name: "All"},
  {id:"Processing", name: "Processing"},
  {id:"ProcessingGroup", name: "Processing Group"},
  {id:"Datum", name: "Data"}
]

SEARCH_DEFAULT = {id: "", name: "All"}

PROCESSING_GROUP_NAME = 'Processing Group'
PROCESSING_NAME = 'Processing'

module = angular.module 'Rosemary'
module.factory 'Object', ->

  new class

    ###########################
    # Instance variables      #
    ###########################

    # Storage for all unique objects/instances
    objects: {}

    ###########################
    # Constructor & init      #
    ###########################

    ###########################
    # Methods                 #
    ###########################

    # This method returns you the ID of anything containing an ID. This can be an instance, reference or ID itself.
    toId: (o) =>
      if not o?
        return false

      if not o?.$$ID$$
        o.$$ID$$ = @_toId o

      o.$$ID$$

    _toId: (o) =>
      if o?._id then o = o._id
      if o?.$oid then o = o.$oid
      if o?.id then o = o.id
      
      if _.isString o then o
      else
        switch @toType o
          when 'processing_bundle' then 'PB_' + @toId o.processing
          else console.error o

    # Compare 2 instances based on their ID's
    equal: (l, r) => @toId(l) is @toId(r)

    equalFn: (l) =>
      id = @toId(l)
      (r) => id is @toId(r)

    # Return the single unique instance of the object
    unique: (obj) =>
      id = @toId obj
      val = @objects[id]
      if _.isUndefined val
        @objects[id] = obj
        val = obj
      else
        _.forEach obj, (v, k) -> val[k] = v
      val

    # Search all unique instances for the object is the list
    uniqueAll: (data) =>
      _.forEach data, (entry, key) =>
        id = @toId entry
        val = @objects[id]
        if _.isUndefined val
          @objects[id] = entry
        else
          _.forEach entry, (v, k) -> val[k] = v
          data[key] = val
      data

    # Compare objects based on contents
    deepEqual: (l, r) ->
      JSON.stringify(l) is JSON.stringify(r)

    # Returns readable type TODO: Add more types
    toType: (o) ->
      if _.has o, '_t'
        switch o._t
          when PROCESSING_GROUP_TYPE then 'processing_group'
          when PROCESSING_TYPE then 'processing'
          when DATUM_TYPE then 'datum'
          else o._t
      else if _.has o, 'processing' then 'processing_bundle'