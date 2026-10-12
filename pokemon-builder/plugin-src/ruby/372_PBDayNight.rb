module PBDayNight
#============================================================
#调用以下方法用于判断现实时间的早午晚，与原本的作用一样
#============================================================
# Returns true if it's day.
  def self.isRealDay?(time=nil)
    time = pbGetTimeNow if !time
    return (time.hour>=6 && time.hour<18)
  end

# Returns true if it's night.
  def self.isRealNight?(time=nil)
    time = pbGetTimeNow if !time
    return (time.hour>=18 || time.hour<6)
  end

# Returns true if it's dawn.
  def self.isRealDawn?(time=nil)
    time = pbGetTimeNow if !time
    return (time.hour>=3 && time.hour<6)
  end
  
# Returns true if it's morning.
  def self.isRealMorning?(time=nil)
    time = pbGetTimeNow if !time
    return (time.hour>=6 && time.hour<9)
  end
  
# Returns true if it's beforeNoon.
  def self.isRealBeforeNoon?(time=nil)
    time = pbGetTimeNow if !time
    return (time.hour>=9 && time.hour<11)
  end

# Returns true if it's at noon.
  def self.isRealAtNoon?(time=nil)
    time = pbGetTimeNow if !time
    return (time.hour>=11 && time.hour<13)
  end
  
# Returns true if it's the afternoon.
  def self.isRealAfternoon?(time=nil)
    time = pbGetTimeNow if !time
    return (time.hour>=13 && time.hour<18)
  end
  
# Returns true if it's the dusk.
  def self.isRealDusk?(time=nil)
    time = pbGetTimeNow if !time
    return time.hour==18
  end

# Returns true if it's the evening.
  def self.isRealEvening?(time=nil)
    time = pbGetTimeNow if !time
    return (time.hour>=19 && time.hour<22)
  end

# Returns true if it's the midnight.
  def self.isRealMidnight?(time=nil)
    time = pbGetTimeNow if !time
    return (time.hour>=22 || time.hour<3)
  end
  
# Returns true if it's the proper time for Rainbow Alcremie
  def self.isRealRainbow?(time=nil)
    time = pbGetTimeNow if !time
    return (time.hour>=19 && time.hour<20)
  end
  
#============================================================
#调用以下方法用于判断游戏内的早午晚（isDay? / isNight? ...），现在与上面的 isReal* 一致，
#都按现实时钟的小时判断（6~17点白天，18~23点和0~5点黑夜），进化、遭遇、战斗背景都用这一套
#============================================================
# Returns true if it's day.
  def self.isDay?(time=nil)
    time = pbGetTimeNow if !time
    hour = time.hour
    return (hour>=6 && hour<18)
  end

# Returns true if it's night.
  def self.isNight?(time=nil)
    time = pbGetTimeNow if !time
    hour = time.hour
    return (hour>=18 || hour<6)
  end

# Returns true if it's dawn.
  def self.isDawn?(time=nil)
    time = pbGetTimeNow if !time
    hour = time.hour
    return (hour>=3 && hour<6)
  end
  
# Returns true if it's morning.
  def self.isMorning?(time=nil)
    time = pbGetTimeNow if !time
    hour = time.hour
    return (hour>=6 && hour<9)
  end
  
# Returns true if it's beforeNoon.
  def self.isBeforeNoon?(time=nil)
    time = pbGetTimeNow if !time
    hour = time.hour
    return (hour>=9 && hour<11)
  end

# Returns true if it's at noon.
  def self.isAtNoon?(time=nil)
    time = pbGetTimeNow if !time
    hour = time.hour
    return (hour>=11 && hour<13)
  end
  
# Returns true if it's the afternoon.
  def self.isAfternoon?(time=nil)
    time = pbGetTimeNow if !time
    hour = time.hour
    return (hour>=13 && hour<18)
  end
  
# Returns true if it's the dusk.
  def self.isDusk?(time=nil)
    time = pbGetTimeNow if !time
    hour = time.hour
    return hour==18
  end

# Returns true if it's the evening.
  def self.isEvening?(time=nil)
    time = pbGetTimeNow if !time
    hour = time.hour
    return (hour>=19 && hour<22)
  end

# Returns true if it's the midnight.
  def self.isMidnight?(time=nil)
    time = pbGetTimeNow if !time
    hour = time.hour
    return (hour>=22 || hour<3)
  end
  
# Returns true if it's the proper time for Rainbow Alcremie
  def self.isRainbow?(time=nil)
    time = pbGetTimeNow if !time
    hour = time.hour
    return (hour>=19 && hour<20)
  end
  
  def self.pbGetDayNightMinutes
    now = pbGetTimeNow   # Get the current in-game time
    minuetes = (now.min * 24 / 60.0).round * 60
    return [minuetes, 24 * 60 - 1].min
  end
  
  def self.pbGetDayNightName
    return "凌晨" if isDawn?
    return "早晨" if isMorning?
    return "上午" if isBeforeNoon?
    return "中午" if isAtNoon?
    return "下午" if isAfternoon?
    return "黄昏" if isDusk?
    return "夜晚" if isEvening?
    return "午夜" if isMidnight?
    return ""
  end
end