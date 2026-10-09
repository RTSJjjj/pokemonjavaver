class PokeBattle_Pokemon
   def hasItem?(item_id = 0)
     held_item = self.item
     return held_item > 0 if item_id == 0
     return held_item == getID(PBItems,item_id)
   end
end

MultipleForms.register(:ARCEUS,{
  "getForm" => proc { |pkmn|
    next nil if !isConst?(pkmn.ability,PBAbilities,:MULTITYPE)
    typeArray = {
       1  => [:FISTPLATE],
       2  => [:SKYPLATE],
       3  => [:TOXICPLATE],
       4  => [:EARTHPLATE],
       5  => [:STONEPLATE],
       6  => [:INSECTPLATE],
       7  => [:SPOOKYPLATE],
       8  => [:IRONPLATE],
       10 => [:FLAMEPLATE],
       11 => [:SPLASHPLATE],
       12 => [:MEADOWPLATE],
       13 => [:ZAPPLATE],
       14 => [:MINDPLATE],
       15 => [:ICICLEPLATE],
       16 => [:DRACOPLATE],
       17 => [:DREADPLATE],
       18 => [:PIXIEPLATE],
       19 => [:LIGHTPLATE],
       20 => [:SHADOWPLATE],
       21 => [:LEGENDPLATE],
    }
    ret = 0
    next 0 if !pkmn.hasItem?
    typeArray.each do |f, items|
      for item in items
        next if !pkmn.hasItem?(item)
        ret = f
        break
      end
      break if ret>0
    end
    next ret
  }
})