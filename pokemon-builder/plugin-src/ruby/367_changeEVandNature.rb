def changeEVandNature(pkmn, changeIV=false)
  pkmn.iv = [31,31,31,31,31,31] if changeIV
  bvs = pkmn.baseStats.clone
  bvs[0] = 0
  evs = [252, 252, 0]
  max = []
  evs.each do |e|
    j = bvs.index(bvs.max)
    pkmn.ev[j] = e
    bvs[j] = 0
    max.push(j)
  end
  pkmn.ev[0] = 6 if pkmn.ev[0] == 0
  nature = :HARDY
  case max[0]
  when 0                        # HP最高
    if max[1]==1 && max[2]!=4   # A次高且SA不第三
      nature = :ADAMANT         # +A -SA
    elsif max[1]==4 && max[2]!=1# SA次高且A不第三
      nature = :MODEST
    end
  when 1                        # A最高
    if [2,3,5].include?(max[1]) # SA不是次高
      nature = :ADAMANT         # +A -SA
    else
      nature = :JOLLY           # +S -SA
    end
  when 2                        # D最高
    if max[1] == 1              # A次高
      nature = :IMPISH          # +D -SA
    else
      if pkmn.baseStats[4] > pkmn.baseStats[1] # SA比A高
        nature = :BOLD          # +D -A
      else
        nature = :IMPISH        # +D -SA
      end
    end
  when 3                        # S最高
    if max[1] == 4              # SA次高
      nature = :MODEST          # +SA -A
    else
      if pkmn.baseStats[4] > pkmn.baseStats[1] # SA比A高
        nature = :MODEST        # +A -SA
      else
        nature = :ADAMANT       # +A -SA
      end
    end
  when 4                        # SA最高
    if [2,3,5].include?(max[1]) # A不是次高
      nature = :MODEST          # +SA -A
    else
      nature = :TIMID           # +S -A
    end
  when 5                        # SD最高
    if max[1] == 3              # SA次高
      nature = :CALM            # +SD -A
    else
      if pkmn.baseStats[4] > pkmn.baseStats[1] # SA比A高
        nature = :CALM          # +SD -A
      else
        nature = :CAREFUL       # +SD -SA
      end
    end
  end
  pkmn.setNature(nature)
  pkmn.calcStats
  return pkmn
end