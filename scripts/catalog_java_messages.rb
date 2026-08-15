#!/usr/bin/env ruby
require "digest"
ROOT=File.expand_path("..",__dir__)
FILES=Dir[File.join(ROOT,"src/main/java/**/*.java")]
PROPERTIES=%w[messages.properties messages_az.properties].map{|f|File.join(ROOT,"src/main/resources",f)}
TRANSLITERATION={"ə"=>"e","Ə"=>"e","ı"=>"i","İ"=>"i","ö"=>"o","Ö"=>"o","ü"=>"u","Ü"=>"u","ş"=>"s","Ş"=>"s","ç"=>"c","Ç"=>"c","ğ"=>"g","Ğ"=>"g"}.freeze
def key_for(text)
  slug=text.chars.map{|c|TRANSLITERATION.fetch(c,c)}.join.downcase.gsub(/[^a-z0-9]+/,"_").gsub(/^_|_$/,"")[0,48]
  "ui.#{slug.empty? ? 'text' : slug}.#{Digest::SHA1.hexdigest(text)[0,6]}"
end
catalog={}
pattern=/(?:addAttribute|addFlashAttribute)\(\s*"(?:pageTitle|successMessage|errorMessage|errorTitle|errorDescription|errorAction)"\s*,\s*"((?:\\.|[^"])*)"/
FILES.each do |path|
  source=File.read(path,encoding:"UTF-8")
  source.scan(pattern){|m|text=m[0].gsub('\\"','"');catalog[key_for(text)]=text}
  if path.include?("/controller/")
    source.scan(/"((?:\\.|[^"])*)"/).each do |m|
      text=m[0].gsub('\\"','"')
      catalog[key_for(text)]=text if text.match?(/[ƏəİıÖöÜüŞşÇçĞğ]/) && !text.include?('${')
    end
  end
end
PROPERTIES.each do |path|
  existing=File.read(path,encoding:"UTF-8");known=existing.lines.map{|l|l[/\A([^#!\s][^=]*)=/,1]}.compact.each_with_object({}){|k,h|h[k]=true}
  additions=catalog.reject{|k,_|known[k]}.sort.map{|k,v|"#{k}=#{v}"}
  File.write(path,existing.rstrip+"\n"+additions.join("\n")+"\n",encoding:"UTF-8") unless additions.empty?
end
puts "#{catalog.size} Java interfeys mətni kataloqlaşdırıldı."
