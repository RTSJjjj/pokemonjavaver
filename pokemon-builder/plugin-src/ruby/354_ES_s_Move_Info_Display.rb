class Move_Info_Display
  
  def initialize(battler, move)
    @viewport   = Viewport.new(0, 48, Graphics.width, Graphics.height-192)
    @viewport.z = 99999
    @battler    = battler
    @move       = move
    @sprites    = {}
    @sprites["bg"]        = Sprite.new(@viewport)
    if $Trainer.female?
      @sprites["bg"].bitmap = Bitmap.new("Graphics/Pictures/Battle/bg_moveinfo_f")
    else
      @sprites["bg"].bitmap = Bitmap.new("Graphics/Pictures/Battle/bg_moveinfo_m")
    end
    @sprites["bg"].x      = 0
    @sprites["bg"].y      = 0
    @sprites["bg"].z      = @viewport.z
    
    @sprites["overlay"]   = BitmapSprite.new(Graphics.width, Graphics.height - 192)
    @sprites["overlay"].x = 0
    @sprites["overlay"].y = 48
    @sprites["overlay"].z = @viewport.z + 1
    pbSetSmallFont(@sprites["overlay"].bitmap)
    
    @oys = [0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 3, 3, 3, 2, 2, 2, 1, 1, 1]
    @oy  = 0
  end
  
  def pbStartScreen
    drawMoveInfo
    index = 0
    loop do
      Graphics.update
      Input.update
      @sprites["bg"].ox += 1
      @sprites["bg"].ox = 0 if @sprites["bg"].ox > 32
      break if Input.trigger?(Input::F5) || Input.trigger?(Input::B)
    end
  end
  
  def drawMoveInfo
    choice = [:UseMove, -1, @battler.battle.struggle, -1]
    targets = @battler.pbFindTargets(choice,@move,@battler)
    target = nil
    targets.each do |t|
      next if !t || t.fainted?
      target = t
      break
    end
    overlay = @sprites["overlay"].bitmap
    base           = Color.new(248, 248, 248)
    shadow         = Color.new( 64,  64,  64)
    textpos = []
    # Get data for move
    moveData = pbGetMoveData(@move.id)
    moveType = @move.pbCalcType(@battler)
    basedamage = moveData[MOVE_BASE_DAMAGE]
    damage     = basedamage
    if target
      basedamage = @move.pbBaseDamage(basedamage,@battler,target) 
      damage     = @move.pbModifyDamage(basedamage,@battler,target)
    end
    category   = @move.category
    accuracy   = moveData[MOVE_ACCURACY]
    accuracy   = @move.pbBaseAccuracy(@battler,target) if target
    priority   = moveData[MOVE_PRIORITY]
    flags      = moveData[MOVE_FLAGS]
    critical   = @move.highCriticalRate? ? 1 : 0
    # 招式名称
    exp_width = overlay.text_size("类别：").width
    moveName = PBMoves.getName(@move.id)
    textpos.push([sprintf("%s",moveName),Graphics.width/4,10,2,base,shadow])
    # 招式属性
    base_x = Graphics.width/2
    textpos.push(["属性：",base_x,10,0,base,shadow])
    typepos = [["Graphics/Pictures/types",base_x+exp_width,6,0,moveType*28,64,28]]
    pbDrawImagePositions(overlay,typepos)
    # 伤害类型
    cate_x = base_x+64+exp_width + 28
    textpos.push(["类别：",cate_x,10,0,base,shadow])
    moveList = [
      getID(PBMoves,:PHOTONGEYSER), getID(PBMoves,:ETERNALFLAME),
      getID(PBMoves,:STELLARBLAST), getID(PBMoves,:TERASTARSTORM)
    ]
    if moveList.include?(@move.id)
      category = @battler.attack>@battler.spatk ? 0 : 1
    end
    catepos = [["Graphics/Pictures/category",cate_x+exp_width,6,0,category*28,64,28]]
    pbDrawImagePositions(overlay,catepos)
    # 基础威力
    textpos.push(["威力：",base_x,36,0,base,shadow])
    move_base = @battler.pbHasType?(moveType) ? Color.new(200, 248, 200) : base
    move_shadow = @battler.pbHasType?(moveType) ? Color.new(0, 200, 40) : shadow
    if basedamage==0   # Status move
      textpos.push(["---",base_x+exp_width,36,0,base,shadow])
    elsif basedamage==1   # Variable power move
      textpos.push(["???",base_x+exp_width,36,0,base,shadow])
    else
      textpos.push([sprintf("%d",basedamage),base_x+exp_width,36,0,move_base,move_shadow])
    end
    # 招式标签
    textpos.push(["标签：",8,36,0,base,shadow])
    flagpos = []
    flags_arr = ["a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","r"]
    flags_x = 60
    flags_arr.each do|flag|
      # 其他flags是有就显示
      no_skip = flags.include?(flag)
      # 守住、鹦鹉学舌是没有才显示
      no_skip = !no_skip if ["b","e"].include?(flag)
      next unless no_skip
      flagpos.push(["Graphics/Pictures/Move Flags/"+flag,flags_x,34,0,0,26,28])
      flags_x += 26
    end
    pbDrawImagePositions(overlay,flagpos)
    # 属性克制
    if target && basedamage && basedamage > 0
      if $DEBUG || $game_switches[197] || target.ownedEx?
        mult = @move.pbCalcTypeMod(moveType,@battler,target)
        if (isConst?(@move.id,PBMoves,:JUDGMENT) || isConst?(@move.id,PBMoves,:GODJUDGMENT)) && @battler.isSpecies?(:ARCEUS) &&
           (@battler.hasActiveItem?(:LEGENDPLATE) || @battler.hasActiveItem?(:LEGENDPLATEFREE))
          mult = (target.type1 != target.type2) ? 32 : 16
        end
        base_good   = Color.new(200, 248, 200)
        shadow_good = Color.new(  0, 200,  64)
        base_bad    = Color.new(248, 192, 192)
        shadow_bad  = Color.new(200,   0,   0)
        base_none   = Color.new(200, 200, 200)
        shadow_none = Color.new( 64,  64,  64)
        offset_x = Graphics.width/4
        ret = 4.0
        if mult == 8 * ret
          textpos.push(["效果绝佳",offset_x,62,2,base_good,shadow_good])
        elsif mult == 8 * 2
          textpos.push(["效果拔群",offset_x,62,2,base_good,shadow_good])
        elsif mult == 8
          textpos.push(["效果正常",offset_x,62,2,base,shadow])
        elsif mult == 8 / 2
          textpos.push(["效果不好",offset_x,62,2,base_bad,shadow_bad])
        elsif mult == 8 / ret
          textpos.push(["收效甚微",offset_x,62,2,base_bad,shadow_bad])
        elsif mult == 0
          textpos.push(["没有效果",offset_x,62,2,base_none,shadow_none])
        end
      end
    end
    # 命中率
    if accuracy==0
      textpos.push(["命中：---",cate_x,36,0,base,shadow])
    else
      textpos.push([sprintf("命中：%d%",accuracy),cate_x,36,0,base,shadow])
    end
    # 会心
    critical += @battler.effects[PBEffects::FocusEnergy]
    textpos.push([sprintf("会心：%d",critical),base_x,62,0,base,shadow])
    # 先制
    priority = -1 if @battler.hasActiveItem?([:LAGGINGTAIL,:FULLINCENSE]) 
    textpos.push([sprintf("先制：%d",priority),cate_x,62,0,base,shadow])
    # 招式描述
    textpos.push(["描述：",8,88,0,base,shadow])
    pbDrawTextPositions(overlay,textpos)
    description = pbGetMessage(MessageTypes::MoveDescriptions,@move.id)
    desc = description.gsub(' ', '')
    length = desc.length
    for ch_i in 0...desc.length
      ch_width = overlay.text_size("字").width
      if ch_width * ch_i >= Graphics.width - 16
        length = ch_i
        break
      end
    end
    desc_arr = desc.scan(/.{1,#{length}}/)
    desc = ""
    desc_arr.each do |r|
      desc += r + "\n"
    end
    drawSmallTextEx(overlay,8,114,Graphics.width-16,3,desc,base,shadow)
  end

  def drawSmallTextEx(bitmap,x,y,width,numlines,text,baseColor,shadowColor,lineheight=26)
    normtext=getLineBrokenChunks(bitmap,text,width,nil,true,lineheight)
    renderLineBrokenChunksWithShadow(bitmap,x,y,normtext,numlines*26,
       baseColor,shadowColor)
  end

  def pbEndScreen
    pbDisposeSpriteHash(@sprites)
    @viewport.dispose
  end
end

class MoveInfoScreen
  def initialize(scene)
    @scene = scene
  end

  def pbShowScreen
    @scene.pbStartScreen
    @scene.pbEndScreen
  end
end

def pbMoveInfo(battler, moveid)
  scene = Move_Info_Display.new(battler, moveid)
  screen = MoveInfoScreen.new(scene)
  screen.pbShowScreen
end

PluginManager.register({
  :name    => "ESMID-影辞定制版",
  :version => "3.1",
  :link    => "https://0vej.esplus.club",
  :credits => ["ES泽洛"]
})
