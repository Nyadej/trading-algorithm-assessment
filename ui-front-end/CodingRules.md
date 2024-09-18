### Each component in its own file

### File name should match component name

### Component name begins with upper case character

### If you need a css file, use same name (but file typpe css)

### root element of each component has css className with same value as component name

### React components are all function components

### use `named` export, not default export

e.g. to start with MarketDepthFeature

create two files

MarketDepthFeature.tsx
MarketDepthFeature.css

MarketDepthFeature.tsx

```
import './MarketDepth.css'

export const MarketDepthFeature = () => {

    return (
        <div className="MarketDepthFeature">
            ... the interesting stuff foes here
        </div>
    )

}
```

MarketDepthFeature.css

```
.MarketDepth {

 background: yellow;
 height: 100%;

}
```
