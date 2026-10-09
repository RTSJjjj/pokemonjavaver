def reset_regional_dex
  old_length = $PokemonGlobal.pokedexUnlocked.length
  new_length = pbDexNames.length
  return if old_length == new_length
  has_national = $PokemonGlobal.pokedexUnlocked[-1]
  for i in 1...new_length
    $PokemonGlobal.pokedexUnlocked[i] = true
  end
  if !has_national
    $PokemonGlobal.pokedexUnlocked[1]  = false
    $PokemonGlobal.pokedexUnlocked[-1] = false
  end
end
#1.密码使用方法：
#passCheck("114514","请输入数字密码?",0,6,false)
def passCheck(password,helptext="请输入数字密码?",minlength=0,maxlength=8,casesensitive=false)
  code=pbEnterText(helptext,minlength,maxlength)
  if code==password || (casesensitive==false && code.downcase==password.downcase)
    return true
  else
    return false
  end
end
def passCheck1(password,helptext="请输入数字密码?",minlength=0,maxlength=8,casesensitive=false)
  code=pbEnterText(helptext,minlength,maxlength)
  if code==password || (casesensitive==false && code.downcase==password.downcase)
    return $game_variables[1] = true
  else
    return $game_variables[1] = false
  end
end

def check_path
  current_path = Dir.pwd
  if current_path.include?("/AppData/Local/Temp")
    pbMessage("请勿直接在压缩包内运行游戏，\n而是解压全部文件后运行。")
    exit
  end
end