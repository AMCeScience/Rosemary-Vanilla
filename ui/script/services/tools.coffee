module = angular.module 'Rosemary'
module.factory 'Tools', () ->

  new class Tools

    ###########################
    # Instance variables      #
    ###########################

    ###########################
    # Constructor & init      #
    ###########################

    ###########################
    # Methods                 #
    ###########################

    tokenizer: (str) -> str
    name_tokenizer: (obj) -> obj.name

    bloodhound: (data, dt = @name_tokenizer, qt = @tokenizer) ->
      bloodhound = new Bloodhound
        datumTokenizer: dt
        queryTokenizer: qt
        local: data

      bloodhound.promise = bloodhound.initialize()
      bloodhound

    plural: (name, amount) ->
      if _.endsWith name, 's'
        name = name.slice 0, -1

      switch amount
        when 0 then 'no ' + name + 's'
        when 1 then 'one ' + name
        else amount + ' ' + name + 's'