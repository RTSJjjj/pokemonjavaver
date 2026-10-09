module PBNatures
  HARDY   = 0
  LONELY  = 1
  BRAVE   = 2
  ADAMANT = 3
  NAUGHTY = 4
  BOLD    = 5
  DOCILE  = 6
  RELAXED = 7
  IMPISH  = 8
  LAX     = 9
  TIMID   = 10
  HASTY   = 11
  SERIOUS = 12
  JOLLY   = 13
  NAIVE   = 14
  MODEST  = 15
  MILD    = 16
  QUIET   = 17
  BASHFUL = 18
  RASH    = 19
  CALM    = 20
  GENTLE  = 21
  SASSY   = 22
  CAREFUL = 23
  QUIRKY  = 24

  def self.maxValue; 24; end
  def self.getCount; 25; end

  def self.getName(id)
    id = getID(PBNatures,id)
    names = [
       _INTL("勤奋"),
       _INTL("怕寂寞"),
       _INTL("勇敢"),
       _INTL("固执"),
       _INTL("顽皮"),
       _INTL("大胆"),
       _INTL("坦率"),
       _INTL("悠闲"),
       _INTL("淘气"),
       _INTL("乐天"),
       _INTL("胆小"),
       _INTL("急躁"),
       _INTL("认真"),
       _INTL("爽朗"),
       _INTL("天真"),
       _INTL("内敛"),
       _INTL("慢吞吞"),
       _INTL("冷静"),
       _INTL("害羞"),
       _INTL("马虎"),
       _INTL("温和"),
       _INTL("温顺"),
       _INTL("自大"),
       _INTL("慎重"),
       _INTL("浮躁")
    ]
    return names[id]
  end

  def self.getStatRaised(id)
    m = (id%25)/5   # 25 here is (number of stats)**2, not PBNatures.getCount
    return [PBStats::ATTACK,PBStats::DEFENSE,PBStats::SPEED,
            PBStats::SPATK,PBStats::SPDEF][m]
  end

  def self.getStatLowered(id)
    m = id%5   # Don't need to %25 here because 25 is a multiple of 5
    return [PBStats::ATTACK,PBStats::DEFENSE,PBStats::SPEED,
            PBStats::SPATK,PBStats::SPDEF][m]
  end

  def self.getStatChanges(id)
    id = getID(PBNatures,id)
    up = PBNatures.getStatRaised(id)
    dn = PBNatures.getStatLowered(id)
    ret = []
    PBStats.eachStat do |s|
      ret[s] = 100
      ret[s] += 10 if s==up
      ret[s] -= 10 if s==dn
    end
    return ret
  end
end
